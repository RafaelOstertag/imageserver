pipeline {
    agent {
        label 'amd64&&freebsd&&kotlin'
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        timestamps()
        disableConcurrentBuilds()
    }

    tools {
        maven 'Latest Maven'
    }

    triggers {
        pollSCM '@hourly'
        cron '@daily'
    }

    stages {
        stage("Build and Test") {
            steps {
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" install'
                }
            }

             post {
                always {
                    junit '**/failsafe-reports/*.xml,**/surefire-reports/*.xml'
                    jacoco()
                }
            }
        }

        stage("Sonarcloud") {
            steps {
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    withSonarQubeEnv(installationName: 'Sonarcloud', credentialsId: 'e8795d01-550a-4c05-a4be-41b48b22403f') {
                        sh label: 'sonarcloud', script: "mvn -B -s \"$MAVEN_SETTINGS_XML\" -Dsonar.branch.name=${env.BRANCH_NAME} $SONAR_MAVEN_GOAL"
                    }
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
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -Psecurity-scan dependency-check:check'
                }
                dependencyCheckPublisher failedTotalCritical: 1, failedTotalHigh: 4, failedTotalLow: 8, failedTotalMedium: 8, pattern: 'target/dependency-check-report.xml', unstableTotalCritical: 0, unstableTotalHigh: 2, unstableTotalLow: 8, unstableTotalMedium: 8
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
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -DskipTests -Dquarkus.package.type=uber-jar deploy'
                }
            }
        }

        stage('Build & Push Development Docker Image') {
            agent {
                label "arm64&&docker&&kotlin"
            }
            when {
                branch 'develop'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                }
                sh "docker build -t rafaelostertag/imageserver:latest -f src/main/docker/Dockerfile.fast-jar ."
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh "docker push rafaelostertag/imageserver:latest"
                }
            }
        }

        stage('Build & Push Release Docker Image') {
            agent {
                label "arm64&&docker&&kotlin"
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                configFileProvider([configFile(fileId: '74b276ff-1dec-4519-9033-51e3fd0eac21', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                }
                sh "docker build -t rafaelostertag/imageserver:${env.VERSION} -f src/main/docker/Dockerfile.fast-jar ."
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh "docker push rafaelostertag/imageserver:${env.VERSION}"
                }
            }
        }

        stage('Trigger k8s deployment') {
            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                build wait: false, job: '../Helm/imageserver', parameters: [string(name: 'VERSION', value: env.VERSION)]
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