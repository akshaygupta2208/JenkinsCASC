import groovy.io.FileType
import hudson.*
import hudson.model.*
@Grab('org.yaml:snakeyaml:1.17')
import jenkins.*
@Grab('org.yaml:snakeyaml:1.17')
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
                                sh "docker login -u ${nexus_username} -p ${nexus_password} https://nexus.softwaremathematics.com/"
                                sh "docker build -t nexus.softwaremathematics.com/${name}:latest ."
                                sh "docker push nexus.softwaremathematics.com/${name}:latest"
                                 }
                    }
                    }
"""
    dev_stage = """
                    stage('DeployDev') { 
                            steps{ 
                                sh 'echo "DeployDev"'
                                sh 'docker run -p 0.0.0.0:${deploy_port}:${application_port} -d nexus.softwaremathematics.com/${name}'
                                
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
