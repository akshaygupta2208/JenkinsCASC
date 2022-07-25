FROM jenkins/jenkins:latest
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false
ENV CASC_JENKINS_CONFIG /var/jenkins_home/casc.yaml
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
WORKDIR /usr/share/jenkins/ref
RUN curl  -L -o jenkins-plugin-manager.jar https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/plugin-management-parent-pom-2.0.0/jenkins-plugin-manager-2.0.0.jar
#RUN ls -l /usr/local/lib #grep jenkins-plugin-manager.jar
RUN java -jar jenkins-plugin-manager.jar -f /usr/share/jenkins/ref/plugins.txt --verbose
COPY --chown=jenkins casc.yaml /var/jenkins_home/casc.yaml
RUN echo 2.0 > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state
