import json
import os
import requests
import yaml
from glob import glob

# NEw IMPORT
from kafka import KafkaConsumer
from kafka.admin import KafkaAdminClient, NewTopic
from kafka.errors import TopicAlreadyExistsError

krakend_base_json = {
    "$schema": "https://www.krakend.io/schema/v3.json",
    "version": 3,
    "max_idle_connections": 300,
    "idle_connection_timeout": "300s",
    "extra_config": {
        "router": {
            "auto_options":True
        },
        "telemetry/logging": {
            "level": "INFO",
            "prefix": "[KRAKEND]",
            "syslog": False,
            "stdout": True,
            "format": "logstash"
        },
        "telemetry/logstash": {
            "enabled": True
        },
        "security/cors": {
            "allow_origins": [
                "*"
            ],
            "expose_headers": [
                "*"
            ],
            "allow_methods": [
                "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
            ],
            "allow_credentials": True,
            "allow_headers": [
                "*"
            ]
        },
        "security/http": {
            "allowed_hosts": [],
            "ssl_proxy_headers": {
                "X-Forwarded-Proto": "https"
            },
            "host_proxy_headers":[
                "X-Forwarded-Hosts"
            ],
            "referrer_policy": "same-origin",
            "content_type_nosniff": True,
            "browser_xss_filter": True,
            "content_security_policy": "default-src 'self';",
            "is_development": True
        }
    },
    "endpoints": []
}

"""
Headers which are to be removed from reaching backend in order to avoid double cors issue
"""


pipeline_base = "jenkins/pipelines"
krakend_base_json_path = "ansible/roles/apithfrole/files"

# enable these below mentioned variables for development in local
pipeline_base = "pipelines"
krakend_base_json_path = "./"


########################


admin_client = KafkaAdminClient(bootstrap_servers=['38.242.198.101:9092', '38.242.198.101:9093', '38.242.198.101:9094'])


topic_names = ['yuvraj']

def create_topics(topic_names):

    existing_topic_list = consumer.topics()
    print(list(consumer.topics()))
    topic_list = []
    for topic in topic_names:
        if topic not in existing_topic_list:
            print('Topic : {} added '.format(topic))
            topic_list.append(NewTopic(name=topic, num_partitions=1, replication_factor=1))
        else:
            print('Topic : {topic} already exist ')
    try:
        if topic_list:
            admin_client.create_topics(new_topics=topic_list, validate_only=False)
            print("Topic Created Successfully")
        else:
            print("Topic Exist")
    except TopicAlreadyExistsError as e:
        print("Topic Already Exist")
    except  Exception as e:
        print(e)
consumer = KafkaConsumer(
    'akshay',
    bootstrap_servers='38.242.198.101:9092',
    auto_offset_reset='earliest'
)
create_topics(topic_names)




def get_recursive_files(base_path):
    result = [y for x in os.walk(base_path) for y in glob(os.path.join(x[0], '*.y*ml'))]
    return result


def read_yaml(yaml_file):
    """ A function to read YAML file"""
    with open(yaml_file) as f:
        config = list(yaml.safe_load_all(f))
    return config


def get_swagger_data(app_base, swagger_uri="/v2/api-docs"):
    # swagger uri = /v2/api-docs
    print(f'Looking for swagger data for http://{app_base}{swagger_uri}')
    try:
        response = requests.get(f'http://{app_base}{swagger_uri}')
        if response.headers.get('content-type') == 'application/json':
            print(f'Swagger data api returned {response.status_code}')
            if response.status_code == 200:
                return response.json()
        else:
            pass
    except requests.exceptions.ConnectionError as e:
        print("Swagger data api is not responding")
    return False


for pipeline_file in get_recursive_files(pipeline_base):
    print(f'Working on file {pipeline_file}')

    # get all necessary details
    yaml_config = read_yaml(pipeline_file)
    # deploy_port
    if "deploy_port" in yaml_config[0] and "deploy_servers_prod" in yaml_config[0]:
        deploy_port = yaml_config[0].get("deploy_port")
        app_name = yaml_config[0].get("name")
        deploy_servers = yaml_config[0].get("deploy_servers_prod")
        print("Application has deploy_port looking for swagger config now")
        # get swagger data if present
        swagger_data = get_swagger_data(f'{deploy_servers[0]}:{deploy_port}')
        if swagger_data:
            # if "tags" in swagger_data:
            #     if "name" in swagger_data["tags"]:
            #         kafka_topic_name = swagger_data["tags"]["name"]
            #         topic_names.append(tag[elem])
            if "paths" in swagger_data:
                for path in swagger_data["paths"]:
                    # generating krakend config here
                    # for each allowed method add an endpoint
                    allowed_methods = list(swagger_data["paths"][path].keys())
                    for method in allowed_methods:
                        method = method.upper()
                        krakend_config = {}
                        krakend_config["endpoint"] = f"/{app_name}{path}"
                        krakend_config["input_query_strings"] = ["*"]
                        if (swagger_data["paths"][path][method.lower()]["tags"][0]).startswith("topic_name"):
                            krakend_config["output_encoding"] = "json"
                        else:
                            krakend_config["output_encoding"] = "no-op"
                        krakend_config["input_headers"] = ["Content-Type", "Cookie"]
                        krakend_config["method"] = method
                        krakend_config["backend"] = []
                        hosts = []
                        for server in deploy_servers:
                            hosts.append(f"{server}:{deploy_port}")
                        if (swagger_data["paths"][path][method.lower()]["tags"][0]).startswith("topic_name"):
                            backend = {
                                "encoding": "json",
                                "url_pattern": path,
                                "host": hosts,
                                "method": method
                            }
                        else:
                            backend = {
                                "encoding": "no-op",
                                "url_pattern": path,
                                "host": hosts,
                                "method": method
                            }
                        krakend_config["backend"].append(backend)

                        if (swagger_data["paths"][path][method.lower()]["tags"][0]).startswith("topic_name"):
                            topic_title = (swagger_data["paths"][path][method.lower()]["tags"][0])
                            topic_name = topic_title.split('topic_name_')
                            topic_names.append(topic_name[1])
                            kafka_relay_backend = {
                                "url_pattern": "/",
                                "host": ["kafka://"],
                                "disable_host_sanitize": True,
                                "extra_config": {
                                    "proxy": {
                                        "shadow": True
                                    },
                                    "backend/pubsub/publisher": {
                                        "topic_url": topic_name[1]
                                    }
                                }
                            }
                            krakend_config["backend"].append(kafka_relay_backend)

                    krakend_base_json["endpoints"].append(krakend_config)
create_topics(topic_names)
# Serializing json
json_object = json.dumps(krakend_base_json, indent=4)
print(json.dumps(json_object, indent=4))
print(topic_names)
# Writing to sample.json
with open(f"{krakend_base_json_path}/krakend.json", "w") as outfile:
    outfile.write(json.dumps(krakend_base_json, indent=4))
