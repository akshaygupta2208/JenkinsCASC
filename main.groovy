job('example') {
  steps {
    shell('echo Hello World!')
  }
}

@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml
def current_workspace = getBinding().getVariables()['WORKSPACE']

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
    steps {
      shell('echo Hello World!')
    }
  }
  println(example)

}
