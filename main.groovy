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

    def jenkinsCredentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.Credentials.class,
            Jenkins.instance,
            null,
            null
    )
    for (creds in jenkinsCredentials) {
        if (creds.id == "nexus") {
            nexus_username = creds.username
            nexus_password = creds.password
        }
    }

    artefact_creation = """
                    stage('ArtefactCreation') { 
                            steps{  
                                sh 'echo "ArtefactCreation"'
                                dir(\"${src_path}\"){ 
                                sh "docker login -u \${NEXUS_CRED_USR} -p \${NEXUS_CRED_PSW} ${NEXUS_REPO_URL}"
                                sh "docker build --network=host -t ${NEXUS_DOCKER_REPO_BASE}/${name}:latest ."
                                sh "docker push ${NEXUS_DOCKER_REPO_BASE}/${name}:latest"
                                 }
                    }
                    }
"""
    dev_stage = """
                    stage('DeployDev') { 
                            steps{ 
                                sh 'echo "DeployDev"'
                                sh 'docker stop ${name}'
                                sh 'docker run --name ${name} -p ${deploy_port}:${application_port} -d ${NEXUS_DOCKER_REPO_BASE}/${name}'
                                
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
                                dir(\"${src_path}\"){  
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

    pipelineJob(example["name"]) {
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
                  stage('Checkout Stage') {     
                        steps{  
                            sh 'echo "Checkout"'
                            git branch: 'main',
                            credentialsId: 'kgyuvraj',
                            url: '${repo_url}'
                            }    
                    }
                    stage('Build') {     
                            steps{
                                dir(\"${src_path}\"){
                                    configFileProvider(
                                        [configFile(fileId: 'mvn-settings', variable: 'MAVEN_SETTINGS')]) {
                                            sh 'echo "Build"'
                                            sh '${build_command} -s \$MAVEN_SETTINGS'                                
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
}


pipelineJob('krakend'){
    definition {
        cps {
            script("""
    pipeline {
                agent any
                tools {
                #maven 'Maven 3'
                #jdk 'openjdk-11'
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
                                  sh 'echo "hello world"'
                                                
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

pipelineJob('jenkins'){
    definition {
        cps {
            script("""
    pipeline {
                agent any
                tools {
                #maven 'Maven 3'
                #jdk 'openjdk-11'
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
                                      sh 'docker login ${NEXUS_REPO_URL}'
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