job('example') {
  steps {
    shell('echo Hello World!')
  }
}

@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml
def current_workspace = getBinding().getVariables()['WORKSPACE']
Yaml parser = new Yaml()
example = parser.load((current_workspace+"/pipelines/a.yaml" as File).text)

println(example)
