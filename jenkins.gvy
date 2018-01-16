pipeline {
    agent {  
      label 'Server' 
    }
    environment {
      CNAME = "ANCHORE_${env.BUILD_NUMBER}"
    }
    stages{
       stage('Checkout') {
           steps{
                git credentialsId: '5f8e03c5-806e-4c4a-ba41-7ce91a97301c',  url: 'git@github.com:stevenfawcett/ruby-test.git'
           }
       }
       stage('Build') {
            steps {
                sh 'docker build . -t localhost:5000/testruby:${BUILD_NUMBER}  -t localhost:5000/testruby:test'
                sh 'docker push localhost:5000/testruby:test'
            }
       }
       stage('Test') {
            parallel {
                stage( 'Sniff tests') {
                    agent {  
                        label 'Server'
                    }
                    steps {
                        sh 'mkdir -p ${WORKSPACE}/smells'
                        sh 'docker run --rm localhost:5000/testruby:test reek --format html app > ${WORKSPACE}/smells/index.html || true'
                    }
                   post {
                        always {
                            publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'smells'  , reportFiles: 'index.html', reportName: 'Code Smell Report', reportTitles: 'Smells'])
                       }
                    }
                }
                stage( 'Unit Tests') {
                    agent {  
                        label 'Server' 
                    }
                    steps {
                        sh 'mkdir -p ${WORKSPACE}/coverage'
                        sh 'docker run --rm -v ${WORKSPACE}/coverage:/usr/src/app/coverage  localhost:5000/testruby:test rails test -v || true '
                    }
                    post {
                        always {
                            publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'coverage', reportFiles: 'index.html', reportName: 'Coverage Report', reportTitles: 'Coverage'])
                        }
                    }
                }
                stage( 'Security Tests') {
                    agent {  
                        label 'Server' 
                    }
                    steps {
                        sh 'mkdir -p ${WORKSPACE}/security'
                        sh 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --name $CNAME anchore/cli:latest || true'
                        sh 'docker exec $CNAME anchore analyze --image localhost:5000/testruby:test || true'
                        sh 'docker exec $CNAME anchore --html  gate    --image localhost:5000/testruby:test         > ${WORKSPACE}/security/gate.html || true'
                        sh 'docker exec $CNAME anchore --html  audit   --image localhost:5000/testruby:test report  > ${WORKSPACE}/security/audit.html || true'
                        sh 'docker stop $CNAME || true'
                        sh 'docker rm $CNAME || true'
                    }
                    post {
                        always {
                            publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'security', reportFiles: 'audit.html,gate.html', reportName: 'Security Report', reportTitles: 'Security'])
                        }
                    }
                }
                stage( 'Selenium Tests') {
                    agent { 
                        label 'Server' 
                    }
                    steps {
                        git credentialsId: '5f8e03c5-806e-4c4a-ba41-7ce91a97301c',  url: 'git@github.com:stevenfawcett/ruby-test.git'
                        sh 'mkdir -p ${WORKSPACE}/selenium'
                        sh 'chmod 777 ${WORKSPACE}/selenium'
                        sh 'cp test.py /${WORKSPACE}/selenium/ || true'
                        dir( '${WORKSPACE}/selenium' )
                        {
                            sh 'docker run --rm --privileged  -v${WORKSPACE}/selenium:/usr/src/app/ selenium nosetests -vs test.py --with-xunit || true'
                        }
                    }
                    post {
                        always {
                            junit '**/nosetests.xml'
                        }
                    }
                }
                
            }
       }
    }
}
