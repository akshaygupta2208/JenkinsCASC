

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
  println(v.getClass())
  println(example["name"])
  pipeline {
    agent any  
      stages {
            stage ('Checkout') {  
              steps {  
                        script {
                          env.currentBuild.displayName = "job21"
                          env.currentBuild.description = "The best description."
                }
                        echo 'Running Checkout phase'  
                }  
            } 
      }
}
  println(example)

}
