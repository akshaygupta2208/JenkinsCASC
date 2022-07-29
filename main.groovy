// Template basic job
// job('example') {
//   steps {
//     shell('echo Hello World!')
//   }
// }

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

dev_stage = """
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
  
  Yaml parser = new Yaml()
  example = parser.load((it.path as File).text)
  repo_url = example["repoUrl"]
  deployenv = example["deployEnv"]
  
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
                            git branch: 'master',
                            credentialsId: 'kgyuvraj',
                            url: '${repo_url}'
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
