job('example') {
  steps {
    shell('echo Hello World!')
  }
}

@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml
def current_workspace = System.getProperty("user.dir");

println(current_workspace)

current_workspace = "/var/jenkins_home/workspace/SeedJob"

import groovy.io.FileType

def list = []


def dev_stage = """
                    stage('DeployDev') { 

                            steps{ 
                                if(deployenv.contains('dev')){
                                sh 'echo "DeployDev"'
                                }}    
                        }
                    stage('DevSanity') {

                            steps{  
                                sh 'echo "DevSanity"'
                                }    
                        }
"""
dev_stage = ""
def dir = new File(current_workspace+"/pipelines")
dir.eachFileRecurse (FileType.FILES) { file ->
  list << file
}

list.each {
  println it.path
  
  Yaml parser = new Yaml()
  example = parser.load((it.path as File).text)
  println(example["name"].getClass())
  def v = example["name"]
  //println(v.getClass())
  println(example["name"])
  some_var = example["repoUrl"]
  buildjob = example["build"]
  deployenv = example["deployEnv"]
  println("this is deploy env "+deployenv)
  println(example["deployEnv"])
  
pipelineJob(example["name"]) {
    definition {
        cps {
            script("""
            pipeline {
                agent any
                stages {
                    stage('Checkout Stage') {     
                        steps{  
                            sh 'echo "Checkout"'
                            sh 'echo $v'
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: '${some_var}'
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
                    stage('ArtefactCreation') {     
                            steps{  
                                sh 'echo "ArtefactCreation"'
                                }    
                        }
                    ${dev_stage}
                    stage('DeployProd') {

                            steps{  
                                sh 'echo "DeployProd"'
                                }    
                        }
                    stage('ProdSanity') {

                            steps{  
                                sh 'echo "ProdSanity"'
                                }    
                        }
                    stage('Deploystg') { 

                            steps{  
                                sh 'echo "DeployDev"'
                                }    
                        }
                    stage('stgSanity') {  

                            steps{  
                                sh 'echo "DevSanity"'
                                }    
                        }
                    stage('Deployqa') { 

                            steps{  
                                sh 'echo "DeployDev"'
                                }    
                        }
                    stage('qaSanity') {

                            steps{  
                                sh 'echo "DevSanity"'
                                }    
                        }
               
            }
        }
                   """)
            sandbox()
        }
    }
}
  println(example)

}
