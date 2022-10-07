# python jenkins/krakend.py
import json
import os
import requests
import yaml
from glob import glob

krakend_base_json = {
    "version": 3,
    "extra_config": {
        "telemetry/logging": {
            "level": "INFO",
            "prefix": "[KRAKEND]",
            "syslog": False,
            "stdout": True,
            "format": "logstash",
            "syslog_facility": "local3"
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
            "debug": True,
            "max_age": "",
            "allow_methods": ["GET", "HEAD", "POST", "OPTIONS", "PATCH", "DELETE"],
            "allow_credentials": True,
            "allow_headers": [
                "*"
            ]
        },
        "security/http": {
            "allowed_hosts": [],
            "ssl_proxy_headers": {
                "X-Forwarded-Proto": "https"
            }
        }
    },
    "endpoints": []
}

"""
Headers which are to be removed from reaching backend in order to avoid double cors issue
"""
disable_cors_backend_headers_json = {
    "modifier/martian": {
        "fifo.Group": {
            "scope": ["request", "response"],
            "aggregateErrors": True,
            "modifiers": [
                {
                    "header.Blacklist": {
                        "scope": ["request"],
                        "names": [
                            "Access-Control-Request-Method",
                            "Sec-Fetch-Dest",
                            "Sec-Fetch-Mode",
                            "Sec-Fetch-Site",
                            "Origin"
                        ]
                    }
                }
            ]
        }
    }
}

pipeline_base = "jenkins/pipelines"
krakend_base_json_path = "ansible/roles/apithfrole/files"


# enable these below mentioned variables for development in local
# pipeline_base = "pipelines"
# krakend_base_json_path = "./"

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
        print(f'Swagger data api returned {response.status_code}')
        if response.status_code == 200:
            return response.json()
    except requests.exceptions.ConnectionError as e:
        print("Swagger data api is not responding")
    return False


for pipeline_file in get_recursive_files(pipeline_base):
    print(f'Working on file {pipeline_file}')

    # get all necessary details
    yaml_config = read_yaml(pipeline_file)
    # deploy_port
    if "deploy_port" in yaml_config[0] and "deploy_servers" in yaml_config[0]:
        deploy_port = yaml_config[0].get("deploy_port")
        app_name = yaml_config[0].get("name")
        deploy_servers = yaml_config[0].get("deploy_servers")
        print("Application has deploy_port looking for swagger config now")
        # get swagger data if present
        swagger_data = get_swagger_data(f'{deploy_servers[0]}:{deploy_port}')
        if swagger_data:
            if "paths" in swagger_data:
                for path in swagger_data["paths"]:
                    # generating krakend config here
                    # for each allowed method add an endpoint
                    for method in swagger_data["paths"][path].keys():
                        method = method.upper()
                        krakend_config = {}
                        krakend_config["endpoint"] = f"/{app_name}{path}"
                        krakend_config["output_encoding"] = "no-op"
                        krakend_config["input_headers"] = ["*"]
                        #krakend_config["method"] = method
                        krakend_config["backend"] = []
                        hosts = []
                        for server in deploy_servers:
                            hosts.append(f"{server}:{deploy_port}")
                        backend = {
                            "encoding": "no-op",
                            "extra_config": disable_cors_backend_headers_json,
                            "url_pattern": path,
                            "host": hosts,
                            #"method": method
                        }
                        krakend_config["backend"].append(backend)
                        krakend_base_json["endpoints"].append(krakend_config)

# Serializing json
json_object = json.dumps(krakend_base_json, indent=4)
print(json.dumps(json_object, indent=4))

# Writing to sample.json
with open(f"{krakend_base_json_path}/krakend.json", "w") as outfile:
    outfile.write(json.dumps(krakend_base_json, indent=4))
