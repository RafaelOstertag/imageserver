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
                script {
                    def version = "undefined"
                    version = sh label: 'Retrieve version', returnStdout: true, script: "grep '^version' build.gradle | sed -e 's/version[[:space:]]//' -e \"s/'//g\""


                    // retriever-connector
                    step([$class                 : "RundeckNotifier",
                          includeRundeckLogs     : true,
                          jobId                  : "f6f97acf-6c9c-4fb1-9b1e-f066869adddc",
                          options                : "version=$version",
                          rundeckInstance        : "gizmo",
                          shouldFailTheBuild     : true,
                          shouldWaitForRundeckJob: true,
                          tailLog                : true])
                }
            }
        }
    }

    post {
        always {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}