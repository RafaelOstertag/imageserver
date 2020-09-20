pipeline {
    agent {
        label 'kotlin'
    }

    environment {
        VERSION= sh label: 'Retrieve version', returnStdout: true, script: "grep '^version' build.gradle | sed -e 's/version[[:space:]]//' -e \"s/'//g\" | tr -d '\\n'"
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

        stage("Nexus Deployment") {
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

        stage("Build & Push Docker Image") {
            agent {
                label "arm64&&docker"
            }

            when {
                allOf {
                    not {
                        triggeredBy 'TimerTrigger'
                    }
                    branch "master"
                }
            }

            steps {
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh "docker build --build-arg 'VERSION=${env.VERSION}' -t rafaelostertag/imageserver:${env.VERSION} docker"
                    sh "docker push rafaelostertag/imageserver:${env.VERSION}"
                }
            }
        }

        stage("Deploy to k8s") {
            agent {
                label "helm"
            }

            when {
                allOf {
                    not {
                        triggeredBy 'TimerTrigger'
                    }
                    branch "master"
                }
            }

            steps {
                withKubeConfig(credentialsId: 'a9fe556b-01b0-4354-9a65-616baccf9cac') {
                    sh """
if ! helm status -n imageserver imageserver
then
  helm install -n imageserver --set image.tag=${env.VERSION} imageserver helm/imageserver
else
  helm upgrade -n imageserver --set image.tag=${env.VERSION} imageserver helm/imageserver
fi
"""
                }
            }
        }
    }

    post {
        unsuccessful {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}