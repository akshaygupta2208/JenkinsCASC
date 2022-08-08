// Template basic job
// job('example') {
//   steps {
//     shell('echo Hello World!')
//   }
// }

@Grab('org.yaml:snakeyaml:1.17')
import jenkins.*
import jenkins.model.* 
import hudson.*
import hudson.model.*
import org.yaml.snakeyaml.Yaml
def current_workspace = System.getProperty("user.dir");

println(current_workspace)

current_workspace = "/var/jenkins_home/workspace/SeedJob"

import groovy.io.FileType

def list = []

def dir = new File(current_workspace+"/pipelines")
dir.eachFileRecurse (FileType.FILES) { file ->
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
  application_port = example["application_port"]
  deploy_port = example["deploy_port"]
  
  
  def jenkinsCredentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.Credentials.class,
        Jenkins.instance,
        null,
        null
);
  for (creds in jenkinsCredentials) {
  if(creds.id == "nexus"){
    println(creds.username)
    println(creds.password)
    }
}
dev_stage = """
                    stage('DeployDev') { 
                            steps{ 
                                sh 'echo "DeployDev"'
                                sh 'docker run -p ${deploy_port}:${application_port} -d nexus.softwaremathematics.com/${name}'
                                
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
  
  
  
  if (! deployenv.contains("dev")){
  dev_stage = ""
  }
  
  if (! deployenv.contains("qa")){
  qa_stage = ""
  }
  
  if (! deployenv.contains("stg")){
  stg_stage = ""
  }
  
  if (! deployenv.contains("prod")){
  prod_stage = ""
  }    
  //println("this is deploy env "+deployenv)
  //println(example["deploy_env"])
  

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
                                sh 'echo "Build"'
                                sh '${build_command}'
                                }    
                        }
                    stage('BuildSanity') {     
                            steps{  
                                sh 'echo "BuildSanity"'
                                }    
                        }
                    stage('ArtefactCreation') { 
                            steps{  
                                sh 'echo "ArtefactCreation"'
                                withCredentials([usernamePassword(credentialsId: 'nexus', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                //sh "echo System.getenv(USERNAME)"
                                sh "docker login -u admin -p kgb https://nexus.softwaremathematics.com/"
                                sh "docker build -t nexus.softwaremathematics.com/petclinic:latest ."
                                sh "docker push nexus.softwaremathematics.com/petclinic:latest"
                    }
                    }
                    }
                    ${dev_stage}
                    ${qa_stage}
                    ${stg_stage}
                    ${prod_stage}    
            }
        }
                   """)
            sandbox()
        }
    }
}

  println(example)

}
