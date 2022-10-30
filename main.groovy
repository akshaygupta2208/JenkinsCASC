@Grab('org.yaml:snakeyaml:1.17')
import groovy.io.FileType
import hudson.*
import hudson.model.*
import jenkins.*
import jenkins.model.*
import org.yaml.snakeyaml.Yaml

def current_workspace = System.getProperty("user.dir")

println(current_workspace)

current_workspace = "/var/jenkins_home/workspace/SeedJob"

def list = []

def dir = new File(current_workspace + "/pipelines")
dir.eachFileRecurse(FileType.FILES) { file ->
    list << file
}

NEXUS_REPO_URL = "https://nexus.softwaremathematics.com/"
NEXUS_DOCKER_REPO_BASE = "nexus.softwaremathematics.com"
VERSION = "${BUILD_TIMESTAMP}"

list.each {
    println it.path

    Yaml parser = new Yaml()
    example = parser.load((it.path as File).text)

    // variable declaration of yaml files
    repo_url = example["repo_url"]
    build_command = example["build_command"]
    java_command = example["run_command"]
    deployenv = example["deploy_env"]
    name = example["name"]
    name = name.toLowerCase()
    application_port = example["application_port"]
    deploy_port = example["deploy_port"]
    src_path = example["src_path"]
    dev_deploy = ""
    prod_deploy = ""
    deploy_envir = ""


    if (example["deploy_servers_dev"] != null) {
        for (server in example["deploy_servers_dev"]) {
            dev_deploy = dev_deploy + """withEnv(["CONTAINER_NAME=${name}","CONTAINER_IMAGE=${NEXUS_DOCKER_REPO_BASE}/${name}", "deploy_port=${deploy_port}", "application_port=${application_port}", "SM_ENV=${example["deploy_env_variable"]}]) {
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', playbook: 'ansible/deployapp.yml', extras: \'-i \"${server},\"\'
           }
            """

        }
    }
    if (example["deploy_servers_prod"] != null) {
        for (server in example["deploy_servers_prod"]) {
            prod_deploy = prod_deploy + """withEnv(["CONTAINER_NAME=${name}","CONTAINER_IMAGE=${NEXUS_DOCKER_REPO_BASE}/${name}", "deploy_port=${deploy_port}", "application_port=${application_port}", "SM_ENV=${example["deploy_env_variable"]}"]) {
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', playbook: 'ansible/deployapp.yml', extras: \'-i \"${server},\"\'
            }            
            """
        }
    }
//    if (example["deploy_env_variable"] != null) {
////        for (var in example["deploy_env_variable"]) {
////            for (i in var) {
//                deploy_envir = deploy_envir + """withEnv(["CONTAINER_NAME=${name}","CONTAINER_IMAGE=${NEXUS_DOCKER_REPO_BASE}/${name}", "deploy_port=${deploy_port}", "application_port=${application_port}", "SM_ENV=${example["deploy_env_variable"]}"]) {
//                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', playbook: 'ansible/deployapp.yml', extras: \'-i \"${server},\"\'
//
//                 }
//            """
////            }
////        }
//    }

    artefact_creation = """
                    stage('ArtefactCreation') { 
                            steps{  
                                sh 'echo "ArtefactCreation"'
                                dir(\"app/${src_path}\"){ 
                                sh "docker login -u \${NEXUS_CRED_USR} -p \${NEXUS_CRED_PSW} ${NEXUS_REPO_URL}"
                                sh "docker build --network=host -t ${NEXUS_DOCKER_REPO_BASE}/${name}:\${VERSION} ."
                                sh "docker push ${NEXUS_DOCKER_REPO_BASE}/${name}:\${VERSION}"
                                 }
                    }
                    }
"""
    dev_stage = """
                    stage('DeployDev') { 
                    
  
                        steps{
//                            timeout(time: 300, unit: 'SECONDS') {
//                            input('Do you want to proceed for production deployment?') 
//                                }
                                sh 'echo "DeployDev"'
                                ${dev_deploy}

                               }
                                
                              
                                }    
                    stage('DevSanity') {
                            steps{  
                                sh 'echo "DevSanity"'
                                }    
                        }
"""
    prod_stage = """
                    stage('DeployProd') {
                            steps{  
                                sh 'echo "DeployProd"'
                                ${prod_deploy}
                                ${deploy_envir}
                                }    
                        }
                    stage('ProdSanity') {
                            steps{  
                                sh 'echo "ProdSanity"'
                                }    
                        }"""
    stg_stage = """
                    stage('Deploystg') { 
                            steps{  
                                sh 'echo "DeployStg"'
                                }    
                        }
                    stage('stgSanity') {  
                            steps{  
                                sh 'echo "StgSanity"'
                                }    
                        }
"""
    qa_stage = """
                    stage('Deployqa') { 
                            steps{  
                                sh 'echo "DeployQA"'
                                
                                }    
                        }
                    stage('qaSanity') {
                            steps{  
                                sh 'echo "QaSanity"'
                                }    
                        }
"""
    mvn_push_stage = """
                    stage('DeployMVN') { 
                            steps{
                                dir(\"app/${src_path}\"){  
                                    configFileProvider(
                                        [configFile(fileId: 'mvn-settings', variable: 'MAVEN_SETTINGS')]) {
                                            sh 'echo "Build"'
                                            sh 'mvn -s \$MAVEN_SETTINGS deploy'
                                        }
                                }
                            }  
                    }
                   
"""


    if (!deployenv.contains("dev")) {
        dev_stage = ""
    }

    if (!deployenv.contains("qa")) {
        qa_stage = ""
    }

    if (!deployenv.contains("stg")) {
        stg_stage = ""
    }

    if (!deployenv.contains("prod")) {
        prod_stage = ""
    }

    if (!deployenv.contains("mvn")) {
        mvn_push_stage = ""

    } else {
        artefact_creation = ""
    }


    repo_url_slash_split = repo_url.split('/')
    folder_name = repo_url_slash_split[repo_url_slash_split.length - 2] + "/" + repo_url_slash_split.last().replace(".git", "").trim()

    // Creating parent folder
    folder(repo_url_slash_split[repo_url_slash_split.length - 2]) {
        description('Folder containing all ' + repo_url_slash_split[repo_url_slash_split.length - 2] + ' related jobs')
    }

    // Creating repo specific folder
    println(folder_name)
    folder(folder_name) {
        description('Folder containing all ' + folder_name + ' related jobs')
    }
    println(folder_name + "/" + example["name"])
    pipelineJob(folder_name + "/" + example["name"]) {
        definition {
            cps {
                script("""
               
            
            pipeline {
                agent any
                tools {
                    maven 'Maven 3'
                    jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                    VERSION = "\${BUILD_TIMESTAMP}"
                }
                stages {
                  stage('Checkout Stage') {     
                        steps{  
                            sh 'echo "Checkout"'
                                                        dir("ansible"){
                                git branch: 'master',
                                credentialsId: 'kgyuvraj',
                                url: 'https://github.com/akshaygupta2208/ansible_repo.git'
                            }
                             dir("app"){
                            git branch: 'main',
                            credentialsId: 'kgyuvraj',
                            url: '${repo_url}'
                            }  }  
                    }
                    stage('Build') {  
                            steps{
                                dir(\"app/${src_path}\"){
                                    configFileProvider(
                                        [configFile(fileId: 'mvn-settings', variable: 'MAVEN_SETTINGS')]) {
                                            sh 'echo "Build"'
                                            sh '${build_command}'                                
                                        }
                                }
                            }    
                    }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                            }    
                    }
                    ${artefact_creation}
                    ${dev_stage}
                    ${qa_stage}
                    ${stg_stage}
                    ${prod_stage}
                    ${mvn_push_stage}
            }
        }
                   """)
                sandbox()
            }
        }
    }
    println(example)


    folder("Infra") {
        description('Folder containing all Infra related jobs')
    }

    pipelineJob('Software-Mathematics/MMUAPI/apithf') {
        definition {
            cps {
                script("""
    pipeline {
                agent any
                tools {
                maven 'Maven 3'
                jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                }
                stages {
                    stage('checkout'){
                        steps{
                  
                            dir("jenkins"){
                                git branch: 'master',
                                credentialsId: 'kgyuvraj',
                                url: 'https://github.com/akshaygupta2208/JenkinsCASC.git'
                            }
                            dir("ansible"){
                                git branch: 'master',
                                credentialsId: 'kgyuvraj',
                                url: 'https://github.com/akshaygupta2208/ansible_repo.git'
                            }
                        }
                    }
                
                    stage('Build') {     
                           steps{                           
                                  sh 'echo "Build"'
                                  sh 'python3 jenkins/krakend.py'
                                }   
                    }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                            }    
                    }
                    stage("execute Ansible") {
           steps {
               
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', inventory: 'ansible/apithf-inventory', playbook: 'ansible/apithf-playbook.yml'
            
               
               }    
        }    
                }
            }
            """)
                sandbox()
            }
        }
    }


    pipelineJob('Infra/jenkins') {
        definition {
            cps {
                script("""
    pipeline {
                agent any
                tools {
                maven 'Maven 3'
                jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                }
                stages {
                    stage('checkout'){
                        steps{
                  
                            dir("jenkins"){
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: 'https://github.com/akshaygupta2208/JenkinsCASC.git'
                            }
                          }
                        }
                
                    stage('Build') {     
                            steps{                           
                                  sh 'echo "Build"'
                                  dir("jenkins"){
                                        sh 'docker build --network=host -t ${NEXUS_DOCKER_REPO_BASE}/jenkins .'
                                  }
                                                
                            }    
                    }
                    stage('deploy') {     
                            steps{                           
                                  sh 'echo "deploy"'
                                  dir("jenkins"){                                
                                      sh "docker login -u \${NEXUS_CRED_USR} -p \${NEXUS_CRED_PSW} ${NEXUS_REPO_URL}"
                                      sh 'docker push ${NEXUS_DOCKER_REPO_BASE}/jenkins'
                                  }           
                            }    
                    }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                              }    
                    }  
                    
                    
            }
            }
            """)
                sandbox()
            }
        }
    }

    pipelineJob('Infra/nginx') {
        definition {
            cps {
                script("""
    pipeline {
                agent any
                tools {
                maven 'Maven 3'
                jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                }
                stages {
                    stage('checkout'){
                        steps{
                  
                            dir("ansible"){
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: 'https://github.com/akshaygupta2208/ansible_repo.git'
                            }
                          }
                        }
                
                    stage('Build') {     
                            steps{                           
                                  sh 'echo "Build"'        
                            }    
                  }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                              }    
                    }  
                    stage("execute Ansible") {
           steps {
               
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', inventory: 'ansible/hosts.yaml', playbook: 'ansible/nginx.yml'
            
               
               }    
        }
                    
                    
            }
            }
            """)
                sandbox()
            }
        }
    }
    pipelineJob('Infra/monitoring-server') {
        definition {
            cps {
                script("""
    pipeline {
                agent any
                tools {
                maven 'Maven 3'
                jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                }
                stages {
                    stage('checkout'){
                        steps{
                  
                            dir("ansible"){
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: 'https://github.com/akshaygupta2208/ansible_repo.git'
                            }
                          }
                        }
                
                    stage('Build') {     
                            steps{                           
                                  sh 'echo "Build"'        
                            }    
                  }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                              }    
                    }  
                    stage("execute Ansible") {
           steps {
               
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', inventory: 'ansible/monitoring-inventory.yml', playbook: 'ansible/monitoring-nginx-playbook'
            
               
               }    
        }
                    
                    
            }
            }
            """)
                sandbox()
            }
        }
    }
    pipelineJob('Infra/create-user') {

        parameters {
            stringParam("USERNAME", "root", "Sample string parameter")
            stringParam('PASSWORD', null, 'Enter the password of the remote host')
            stringParam("IP", null, "Sample string parameter")
        }
        definition {

            cps {
                script("""
    pipeline {
                agent any
                tools {
                maven 'Maven 3'
                jdk 'openjdk-11'
                }
                environment {
                    NEXUS_CRED = credentials('nexus')
                }
                stages {
                    stage('checkout'){
                        steps{
                  
                            dir("ansible"){
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: 'https://github.com/akshaygupta2208/ansible_repo.git'
                            }
                          }
                        }
                
                    stage('Build') {     
                            steps{                           
                                  sh 'echo "Build"'        
                            }    
                  }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                              }    
                    }  
                    stage("execute Ansible") {
           steps {
               
                withEnv(["CONTAINER_NAME=department-service","CONTAINER_IMAGE=nexus.softwaremathematics.com/department-service", "deploy_port=9085", "application_port=9000"]) {
                ansiblePlaybook credentialsId: 'private-key', disableHostKeyChecking: true, installation: 'Ansible', playbook: 'ansible/createuser.yml', extras: '--extra-vars "ansible_user=\${USERNAME} ansible_password=\${PASSWORD}" -i "\${IP},"'
            
               
               }    
        }
                    }
                    
            }
            }
            """)
                sandbox()
            }
        }
    }


}