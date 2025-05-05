## INSTALL RANCHER DESKTOP WHICH COMES WITH KUBERNETES

### ####################################################################
### Step 1: Deploy selenium grid in kubernetes using below commands.
### ####################################################################

# Deploy the namespace:
	kubectl apply -f selenium.namespace.yml

# Deploy the Hub node:
	kubectl apply -f selenium-hub-deployment.yml

# Deploy hub service so nodes can talk to hub node:
	kubectl apply -f selenium-hub-svc.yml

# If you want Chrome nodes:
	kubectl apply -f selenium-node-chrome-deployment.yml

# Edge Node
	kubectl apply -f selenium-node-edge-deployment.yml

### ####################################################################
### Step 2: Deploy ftp server in kubernetes which can be used to see the reports generated.
### ####################################################################

### This will be a local docker image, NOT from internet.
# First
	docker pull stilliard/pure-ftpd
# Second
	kubectl apply -f ftp-server-deployment.yml
# Third check the node port.
	kubectl get services -n selenium
# Connect to this ftp server from winscp. Open winscp use "ftp", hostname "localhost" port whatever given in above command, username "user1" password "password1"

### ####################################################################
### Step 3: Deploy waf image (having framework and test cases ready to run) in kubernetes which can be used to see the reports generated.
### ####################################################################

### This will be a local docker image, NOT from internet. First build the docker image from docker file Df-waf-2stage-build-deploy-root.txt. This also uses file entrypoint_root.sh, so make sure the file is also in same directory.

# First build the image - this will download code from git repo and compile it and create image ready for running the tests.
	## use tag name as waf2st as this is the same referenced in waf-deployment.yml, if different name is used ensure same in yml file.
	## If using powershell
		$CACHEBUST = (Get-Date -UFormat %s)
		docker build -t <tag-name> --build-arg CACHEBUST=$CACHEBUST -f Df-waf-2stage-build-deploy-root.txt .
	## If using command prompt
		for /f %i in ('powershell -Command "(Get-Date -UFormat %s)"') do set CACHEBUST=%i
		docker build -t <tag-name> --build-arg CACHEBUST=%CACHEBUST% -f Df-waf-2stage-build-deploy-root.txt .

# Second deploy to kubernetes
	kubectl apply -f waf-deployment.yml

# LOOK AT YML FILES FOR ANY PARAMATER TUNING

	kubectl scale --replicas=1 -f selenium-node-chrome-deployment.yml
	kubectl rollout restart deployment selenium-node-edge -n selenium
	kubectl port-forward $POD_NAME 5900:5900
	kubectl delete deployment selenium-hub -n selenium
	kubectl delete deployment selenium-node-chrome -n selenium
	kubectl delete deployment selenium-node-firefox -n selenium
	kubectl delete deployment selenium-python -n selenium
	kubectl delete svc selenium-hub -n selenium
	kubectl exec -it <podname> -n selenium -- ls -l /home/user1
	kubectl get pods -n selenium --watch
	kubectl describe pod ftp-server-79846b8989-qt88v -n selenium
	kubectl logs ftp-server-79846b8989-qt88v -n selenium
	kubectl logs -f waf2st-deployment-8f69df6f9-nx2mb -n selenium