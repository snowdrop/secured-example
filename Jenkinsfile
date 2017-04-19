node("launchpad-maven") {
  checkout scm
  def sso_url
  stage("Deploy SSO") {
    sh "cd sso; mvn fabric8:deploy"
    sh 'pwd'
    SSO_URL = sh (
      script: 'cd sso; java -jar target/sso-client.jar --displaySSOURL',
      returnStdout: true
    ).trim()
    SSO_URL = ( SSO_URL =~ /Using auth server URL: (.+)/)
    SSO_URL = SSO_URL[0][1]
  }
  stage("Build") {
    sh "cd app; mvn -DSSO_AUTH_SERVER_URL=${SSO_URL} fabric8:deploy -Popenshift -DskipTests"
  }
}

