

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
                    stage('DeployDev') { 
                                if (deployenv.contains('dev')) {
                                      println("dev branching")
                                  
                            steps{  
                                sh 'echo "DeployDev"'
                                } 
                                }   
                        }
                    stage('DevSanity') {
                          if (deployenv.contains('dev')) {
                                println("dev branching")
                            
                            steps{  
                                sh 'echo "DevSanity"'
                                }
                                }    
                        }
                    stage('DeployProd') {
                            if (deployenv.contains('prod')) {
                                  println("prod branching")
                              
                            steps{  
                                sh 'echo "DeployProd"'
                                }
                                }
                        }
                    stage('ProdSanity') {
                        if (deployenv.contains('prod')) {
                              println("prod branching")
                          
                            steps{  
                                sh 'echo "ProdSanity"'
                                }
                                }
                        }
                    stage('Deploystg') { 
                           if (deployenv.contains('stg')) {
                                  println("stg branching")
                              
                            steps{  
                                sh 'echo "DeployDev"'
                                }
                                }
                        }
                    stage('stgSanity') {  
                      if (deployenv.contains('stg')) {
                      println("stg branching")
                  
                            steps{  
                                sh 'echo "DevSanity"'
                                }
                                }
                        }
                    stage('Deployqa') { 
                    if (deployenv.contains('stg')) {
                          println("qa branching")
                      
                            steps{  
                                sh 'echo "DeployDev"'
                                } 
                                }
                        }
                    stage('qaSanity') {
                     if (deployenv.contains('stg')) {
                          println("stg branching")
                      
                            steps{  
                                sh 'echo "DevSanity"'
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
  println(example)

}
