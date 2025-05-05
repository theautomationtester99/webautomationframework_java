Step 1
------
in the code ensure the ftp related functions are enabled.

Step 2
------
docker pull stilliard/pure-ftpd
docker run -d --name ftp_server -p 21:21 -p 30000-30009:30000-30009 -e FTP_USER_NAME=user1 -e FTP_USER_PASS=password1 -e FTP_USER_HOME=/home/user1 -v C:\docker-ftp-folder:/home/user1 stilliard/pure-ftpd

Step 3
------
Connect with winscp and check use "ftp", hostname "localhost" port whatever given in above command, username "user1" password "password1"

Step 4
------
Build the docker image from docker file Df-waf-2stage-build-deploy-root.txt. This also uses file entrypoint_root.sh, so make sure the file is also in same directory.

# First build the image - this will download code from git repo and compile it and create image ready for running the tests.
	## use tag name as waf2st as this is the same referenced in waf-deployment.yml, if different name is used ensure same in yml file.
	## If using powershell
		$CACHEBUST = (Get-Date -UFormat %s)
		docker build -t <tag-name> --build-arg CACHEBUST=$CACHEBUST -f Df-waf-2stage-build-deploy-root.txt .
	## If using command prompt
		for /f %i in ('powershell -Command "(Get-Date -UFormat %s)"') do set CACHEBUST=%i
		docker build -t <tag-name> --build-arg CACHEBUST=%CACHEBUST% -f Df-waf-2stage-build-deploy-root.txt .

give tag name as waf2st as used in commands in step 6

Step 5
------
Get the ipaddress of ftp container.

docker inspect 062c3b10fe7ec82a0cb250a08e08164f83788282e821c3aaccf6225a5c2723cb

in output copy the ip address of ftp container.

Step 6
------
launch the waf running containers many if require like this

docker run -d --rm -e NO_THREADS=3 -e PARALLEL_EXECUTION=yes -e FTP_UPLOAD=yes -e FTP_HOST=172.17.0.2 -e LOG_LEVEL=warn waf2st
docker run -d --rm -e NO_THREADS=3 -e PARALLEL_EXECUTION=yes -e FTP_UPLOAD=yes -e FTP_HOST=172.17.0.2 -e LOG_LEVEL=warn -e HEADLESS=no waf2st