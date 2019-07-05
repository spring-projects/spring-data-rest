pipeline {
    agent none

    triggers {
        pollSCM 'H/10 * * * *'
        upstream(upstreamProjects: "spring-hateoas/0.25.x, spring-data-cassandra/1.5.x,spring-data-couchbase/2.2.x,spring-data-elasticsearch/2.1.x,spring-data-gemfire/1.9.x,spring-data-jpa/1.11.x," +
                "spring-data-ldap/1.0.x,spring-data-mongodb/1.10.x,spring-data-neo4j/4.2.x,spring-data-redis/1.8.x,spring-data-solr/2.1.x", threshold: hudson.model.Result.SUCCESS)
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '14'))
    }

    stages {
        stage("Test") {
            when {
                anyOf {
                    branch '2.6.x'
                    not { triggeredBy 'UpstreamCause' }
                }
            }
            parallel {
                stage("test: baseline") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            label 'data'
                            args '-v $HOME:/tmp/jenkins-home'
                        }
                    }
                    options { timeout(time: 30, unit: 'MINUTES') }
                    steps {
                        sh 'rm -rf ?'
                        sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw clean dependency:list test -Dsort -B'
                    }
                }
            }
        }
        stage('Release to artifactory') {
            when {
                branch 'issue/*'
                not { triggeredBy 'UpstreamCause' }
            }
            agent {
                docker {
                    image 'adoptopenjdk/openjdk8:latest'
                    label 'data'
                    args '-v $HOME:/tmp/jenkins-home'
                }
            }
            options { timeout(time: 20, unit: 'MINUTES') }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh 'rm -rf ?'
                sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -Pci,artifactory ' +
                        '-Dartifactory.server=https://repo.spring.io ' +
                        "-Dartifactory.username=${ARTIFACTORY_USR} " +
                        "-Dartifactory.password=${ARTIFACTORY_PSW} " +
                        "-Dartifactory.staging-repository=libs-snapshot-local " +
                        "-Dartifactory.build-name=spring-data-rest-2.6 " +
                        "-Dartifactory.build-number=${BUILD_NUMBER} " +
                        '-Dmaven.test.skip=true clean deploy -B'
            }
        }
        stage('Release to artifactory with docs') {
            when {
                branch '2.6.x'
            }
            agent {
                docker {
                    image 'adoptopenjdk/openjdk8:latest'
                    label 'data'
                    args '-v $HOME:/tmp/jenkins-home'
                }
            }
            options { timeout(time: 20, unit: 'MINUTES') }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh 'rm -rf ?'
                sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -Pci,artifactory ' +
                        '-Dartifactory.server=https://repo.spring.io ' +
                        "-Dartifactory.username=${ARTIFACTORY_USR} " +
                        "-Dartifactory.password=${ARTIFACTORY_PSW} " +
                        "-Dartifactory.staging-repository=libs-snapshot-local " +
                        "-Dartifactory.build-name=spring-data-rest-2.6 " +
                        "-Dartifactory.build-number=${BUILD_NUMBER} " +
                        '-Dmaven.test.skip=true clean deploy -B'
            }
        }
    }

    post {
        changed {
            script {
                slackSend(
                        color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
                        channel: '#spring-data-dev',
                        message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
                emailext(
                        subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
                        mimeType: 'text/html',
                        recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                        body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
            }
        }
    }
}
