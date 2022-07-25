job('example') {
  steps {
    shell('echo Hello World!')
  }
}

@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml

Yaml parser = new Yaml()
List example = parser.load(($WORKSPACE+"/pipelines/a.yaml" as File).text)

example.each{println it.subject}
