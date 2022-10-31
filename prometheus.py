# python jenkins/krakend.py
import json
import os
import requests
import yaml
from glob import glob
import requests
import json
from collections import OrderedDict
import collections
import oyaml as yaml

dev_item_list = []



pipeline_base = "jenkins/pipelines"
krakend_base_json_path = "ansible/roles/apithfrole/files"

# enable these below mentioned variables for development in local
#pipeline_base = "pipelines"
#krakend_base_json_path = "./"


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
class MyDumper(yaml.Dumper):

    def increase_indent(self, flow=False, indentless=False):
        return super(MyDumper, self).increase_indent(flow, False)


def write_yaml(yaml_data):
    """ A function to write YAML file"""
    with open('toyaml_updated_stg.yml', 'w') as f:
        yaml.dump(yaml_data, f, Dumper=MyDumper, default_flow_style=False)

def health_check_is_available(domain):
    try:
        x = requests.get("https://" + domain + "/app/health")
        if x.status_code == 200:
            return True
    except Exception as e:
        print(e)
    return False


yaml_dict = OrderedDict({"scrape_configs": [
    {
        "job_name": "blackbox",
        "static_configs": [
            OrderedDict({
                "targets": [],
                "labels": OrderedDict({
                    "service_name": "",
                    "env": "",
                    "verticle": ""
                })

            })
        ]
    }
]

}
)

def write_data_yaml(item_list,env):
    for item in item_list:
        ### exclude RGB environments
        exclude_list = ["-red-", "-blue-", "-green-"]
        if [ele for ele in exclude_list if(ele in item)]:
            continue
        ###### check if monitoring is not enabled
        if True: #item not in data:
            namespace = item.split(',')[-1]
            service_end_point = item.split(',')[0]
            service_name = item.split(".")[0].split("-")[0]
            #print(namespace,service_end_point,service_name)
            ####check if healthcheck endpoint is available
            if health_check_is_available(service_end_point):
                #### identify the service name

                service_name_found = False

                for idx, data_item in enumerate(yaml_dict["scrape_configs"][0]["static_configs"]):
                    if data_item["labels"]["service_name"] == service_name:
                        service_name_found = True

                        yaml_dict["scrape_configs"][0]["static_configs"][idx]["targets"].append(
                            "https://" + service_end_point + "/app/health")
                        break
                if not service_name_found:
                    new_data_item = OrderedDict({
                        "targets": ["https://" + service_end_point + "/app/health"],
                        "labels": OrderedDict({
                            "service_name": service_name,
                            "env": env,
                            "verticle": namespace
                        })
                    })
                    yaml_dict["scrape_configs"][0]["static_configs"].append(new_data_item)
            else:
                print("health check is not available for " + item)


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
            write_data_yaml(dev_item_list, 'dev')
