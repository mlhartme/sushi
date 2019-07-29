pipeline { 
    agent { 
        docker {
            image 'javabuild'
            args '-v $HOME/.m2/repository:/root/.m2/repository'
        }
    }
    environment {
        ARTIFACTORY_USER = credentials('ARTIFACTORY_USER')
        ARTIFACTORY_PASSWORD = credentials('ARTIFACTORY_PASSWORD')
    }
    stages { 
        stage('Build') { 
            steps { 
               sh 'mvn -B clean verify'
            }
        }
    }
    post {
        always {
            junit 'build/reports/**/*.xml'
        }
    }
}

