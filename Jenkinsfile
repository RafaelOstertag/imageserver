pipeline {
    agent {
        label 'kotlin'
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')
        timestamps()
    }

  triggers {
    cron '@daily'
  }

  stages {
    stage("Build & test") {
      steps {
        sh "./gradlew test"
      }
    }

    stage("Deployment") {
      when {
          allOf {
              not {
                  triggeredBy 'TimerTrigger'
              }
              branch "master"
          }
      }

      steps {
          withCredentials([usernamePassword(credentialsId: '023b356d-2960-4b97-9d55-9e621c0b7461', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
              sh './gradlew publish -PpublishUser="${USERNAME}" -PpublishPassword="${PASSWORD}"'
          }
      }
    }
  }
}