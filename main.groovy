

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

def dir = new File(current_workspace+"/pipelines")
dir.eachFileRecurse (FileType.FILES) { file ->
  list << file
}

list.each {
  println it.path
  
  Yaml parser = new Yaml()
  example = parser.load((it.path as File).text)
  job(example["name"]) {
    pipeline {  
    agent any  
    stages {
            stage ('Checkout') {  
              steps {  
                        echo 'Running Checkout phase'  
                }  
            } 
            stage ('Build') {  
              steps {  
                        echo 'Running Build phase...'  
                }  
            }  
            stage ('BuildSanity') {  
              steps {  
                        echo 'Running BuildSanity phase...'  
                }  
            }  
            stage ('ArtefactCreation') {  
              steps {  
                        echo 'Running ArtefactCreation phase...'  
                }  
            }  
            stage ('DeployDev') {  
              steps {  
                        echo 'Running DeployDev phase...'  
                }    
            }  
            stage ('DevSanity') {  
              steps {  
                        echo 'Running DevSanity phase...'  
                }    
            }  
            stage ('DeployProd') {  
              steps {  
                        echo 'Running DeployProd phase...'  
                }    
            }  
            stage ('ProdSanity') {  
              steps {  
                        echo 'Running ProdSanity phase...'  
                }    
            }  
    }  
}
  println(example)

}
