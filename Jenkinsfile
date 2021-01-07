pipeline {
    agent {
        label 'linux&&kotlin'
    }

    environment {
        VERSION= sh label: 'Retrieve version', returnStdout: true, script: "grep '^version' build.gradle | sed -e 's/version[[:space:]]//' -e \"s/'//g\" | tr -d '\\n'"
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        timestamps()
        disableConcurrentBuilds()
    }

    triggers {
        pollSCM '@hourly'
        cron '@daily'
    }

    stages {
        stage("Build & test") {
            steps {
                sh "./gradlew -Dorg.gradle.daemon=false build check"
            }

             post {
                always {
                    junit '**/test-results/test/*.xml'
                    jacoco()
                }
            }
        }

        stage("Sonarcloud") {
            steps {
                withSonarQubeEnv(installationName: 'Sonarcloud', credentialsId: 'e8795d01-550a-4c05-a4be-41b48b22403f') {
                    sh "./gradlew -Dorg.gradle.daemon=false -Dsonar.branch.name=${env.BRANCH_NAME} sonarqube"
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage("Check Dependencies") {
            steps {
                sh './gradlew -Dorg.gradle.daemon=false dependencyCheckAnalyze'
                dependencyCheckPublisher failedTotalCritical: 1, failedTotalHigh: 5, failedTotalLow: 8, failedTotalMedium: 8, pattern: '**/dependency-check-report.xml', unstableTotalCritical: 0, unstableTotalHigh: 4, unstableTotalLow: 8, unstableTotalMedium: 8
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
                    sh './gradlew -Dorg.gradle.daemon=false publish -PpublishUser="${USERNAME}" -PpublishPassword="${PASSWORD}"'
                }
            }
        }

        stage('Trigger k8s deployment') {
            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                build wait: false, job: '../docker/imageserver', parameters: [string(name: 'VERSION', value: env.VERSION), booleanParam(name: 'DEPLOY', value: true)]
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