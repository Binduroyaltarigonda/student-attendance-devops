// End-to-End DevOps Pipeline for the Online Student Attendance System
// GitHub (source) -> Jenkins (CI/CD) -> Maven (build + test) -> Ansible (deploy)

pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy the build with Ansible after tests pass')
        choice(name: 'TARGET', choices: ['local', 'servers'], description: 'Inventory group to deploy to (see ansible/inventory.ini)')
    }

    triggers {
        // Checks GitHub every 2 minutes for new commits. If Jenkins is reachable from the internet,
        // you can switch to a GitHub webhook instead: githubPush()
        pollSCM('H/2 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        // Jenkins on macOS (Homebrew) often starts without Homebrew on its PATH
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        APP_NAME = 'student-attendance'
        ARTIFACT = 'target/student-attendance.jar'
        APP_URL  = 'http://localhost:8081'
        ANSIBLE_FORCE_COLOR = 'true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty=format:"Commit %h by %an: %s"'
            }
        }

        stage('Tool Versions') {
            steps {
                sh '''
                    java -version
                    mvn -version
                    ansible --version | head -1
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit allowEmptyResults: false, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                // jacoco:check fails the build if line coverage drops below the minimum in pom.xml
                sh 'mvn -B -ntp jacoco:report jacoco:check@check'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -ntp package -DskipTests'
                archiveArtifacts artifacts: "${ARTIFACT}", fingerprint: true
            }
        }

        stage('Deploy with Ansible') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                // dontKillMe stops Jenkins from killing the app after the build finishes (local deploys)
                withEnv(['JENKINS_NODE_COOKIE=dontKillMe', 'BUILD_ID=dontKillMe']) {
                    sh """
                        cd ansible
                        ansible-playbook deploy.yml \
                            --limit ${params.TARGET} \
                            -e artifact_path=${WORKSPACE}/${ARTIFACT} \
                            -e app_version=${BUILD_NUMBER} \
                            -e git_commit=\$(git rev-parse --short HEAD)
                    """
                }
            }
        }

        stage('Smoke Test') {
            when {
                allOf {
                    expression { return params.DEPLOY }
                    expression { return params.TARGET == 'local' }
                }
            }
            steps {
                sh '''
                    curl -fsS "$APP_URL/actuator/health" | tee health.json
                    grep -q '"status":"UP"' health.json
                    curl -fsS -o /dev/null -w "Dashboard HTTP %{http_code}\\n" "$APP_URL/"
                    curl -fsS "$APP_URL/api/reports/dashboard"
                '''
            }
        }
    }

    post {
        success {
            echo "Build #${BUILD_NUMBER} passed. App: ${APP_URL}"
        }
        failure {
            echo "Build #${BUILD_NUMBER} failed — check the stage logs above."
        }
        always {
            sh 'rm -f health.json || true'
        }
    }
}
