# python jenkins/prometheus.py
# imported libraries
import os
import yaml
from glob import glob
import requests
from collections import OrderedDict
import oyaml as yaml

dev_item_list = []
infra_endpoints = {
    "Grafana": "https://grafana.softwaremathematics.com",
    "Jenkins": "https://jenkins.softwaremathematics.com/login?from=%2F",
    "Apithf": "https://apithf.softwaremathematics.com/__health",
    "Nexus":  "https://nexus.softwaremathematics.com",
    "Camunda": "https://workflow.softwaremathematics.com/camunda/app/welcome/default/#!/login"
}
pipeline_base = "jenkins/pipelines"
prometheus_path = "ansible/roles/prometheus/files/"

# enable these below mentioned variables for development in local
pipeline_base = "pipelines"
prometheus_path = "./"


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

class MyDumper(yaml.Dumper):

    def increase_indent(self, flow=False, indentless=False):
        return super(MyDumper, self).increase_indent(flow, False)


def write_yaml(yaml_data):
    """ A function to write YAML file"""
    with open(f'{prometheus_path}prometheus.yml', 'w') as f:
        yaml.dump(yaml_data, f, Dumper=MyDumper, default_flow_style=False)

yaml_dict = OrderedDict({
         "static_configs": []
}
)

prometheus_temp = read_yaml(f"{prometheus_path}prometheus.template.yml")
for pipeline_file in get_recursive_files(pipeline_base):
    print(f'Working on file {pipeline_file}')

    # get all necessary details
    yaml_config = read_yaml(pipeline_file)
    job_state = yaml_config[0].get("enabled")
    if (job_state):
        print("configuration generated")
        # deploy_port
        if "deploy_servers_dev" in yaml_config[0]:
            deploy_port = yaml_config[0].get("deploy_port")
            app_name = yaml_config[0].get("name")
            deploy_servers = yaml_config[0].get("deploy_servers_dev")
            print("Application has deploy_port looking for swagger config now")
            # get swagger data if present
            swagger_data = get_swagger_data(f'{deploy_servers[0]}:{deploy_port}')
            if swagger_data:
                service_name_found = False
                if not service_name_found:
                    new_data_item = OrderedDict({
                        "targets": [f"http://{deploy_servers[0]}:{deploy_port}/v2/api-docs"],
                        "labels": OrderedDict({
                            "service_name": app_name,
                            "env": "dev",
                            "bu": "mmu"
                        })
                    })
                    yaml_dict["static_configs"].append(new_data_item)

        if "deploy_servers_prod" in yaml_config[0]:
            deploy_port = yaml_config[0].get("deploy_port")
            app_name = yaml_config[0].get("name")
            deploy_servers = yaml_config[0].get("deploy_servers_prod")
            print("Application has deploy_port looking for swagger config now")
            # get swagger data if present
            swagger_data = get_swagger_data(f'{deploy_servers[0]}:{deploy_port}')
            if swagger_data:
                service_name_found = False
                if not service_name_found:
                    new_data_item = OrderedDict({
                        "targets": [f"http://{deploy_servers[0]}:{deploy_port}/v2/api-docs"],
                        "labels": OrderedDict({
                            "service_name": app_name,
                            "env": "prod",
                            "bu": "mmu"
                        })
                    })

                    yaml_dict["static_configs"].append(new_data_item)
    else:
        print("enabled is false hence not generated")
        pass
for x in infra_endpoints:
    new_data_item = OrderedDict({
        "targets": [infra_endpoints[x]],
        "labels": OrderedDict({
            "service_name": x,
            "env": "prod",
            "bu": "mmu"
        })
    })
    yaml_dict["static_configs"].append(new_data_item)

prometheus_temp[0]["scrape_configs"][1]["static_configs"] = yaml_dict["static_configs"]
prometheus_temp = prometheus_temp[0]
write_yaml(prometheus_temp)
