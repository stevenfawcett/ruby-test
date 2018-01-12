pipeline  {
  agent any
  environment {
      CNAME = "ANCHORE_${env.BUILD_NUMBER}"
  }
  stages{
   stage('Preparation') { 
       steps{
            git credentialsId: '5f8e03c5-806e-4c4a-ba41-7ce91a97301c', 
                url: 'git@github.com:stevenfawcett/ruby-test.git'
       }
   }
   stage('Docker Build') {
        steps {
            sh 'docker build . -t localhost:5000/testruby:${BUILD_NUMBER}  -t localhost:5000/testruby:test'
            sh 'docker push localhost:5000/testruby:test'
        }
   }
   stage('Code Sniff Test') {
        steps {
            sh 'docker run --rm localhost:5000/testruby:test reek app || true'
        }
   }
   stage('Unit Test') {
        steps {
            sh 'docker run --rm localhost:5000/testruby:test rails test -v '
        }
   }
   stage( 'Container Scanner') {
       steps {
           sh 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --name $CNAME anchore/cli:latest'
           sh 'docker exec $CNAME anchore analyze --image localhost:5000/testruby:test'
           sh 'docker exec $CNAME anchore gate    --image localhost:5000/testruby:test || true'
           sh 'docker exec $CNAME anchore audit   --image localhost:5000/testruby:test report || true'
           sh 'docker stop $CNAME'
           sh 'docker rm $CNAME'
       }
   }
  }
}
