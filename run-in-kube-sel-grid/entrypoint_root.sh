#!/bin/bash

# Capture environment variables and store them for cron
printenv | sed 's/^\(.*\)$/export \1/g' > /root/env_vars.sh;

# Restart cron to apply changes
service cron restart

tail -f /dev/null
