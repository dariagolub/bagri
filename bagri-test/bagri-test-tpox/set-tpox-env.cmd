
:config

rem set TPoX HOME properly!

if "%TPOX_HOME%"=="" set TPOX_HOME=C:\Work\Bagri\TPoX

set app_home=.

rem specify the JVM heap size
rem set memory=1024m
set memory=1024m

rem specify schema and admin hosts:ports
set admin_addr=localhost:3330
set schema_addr=localhost:10500
rem set schema_addr=192.168.99.100:10500,192.168.99.100:10501

set login=admin
set password=password
set schema=default

:start
if "%java_home%"=="" (set java_exec=java) else (set java_exec=%java_home%\bin\java)

:launch

set java_opts=-Xms%memory% -Xmx%memory% 

rem set java_opts=%java_opts% -Dtangosol.coherence.proxy.address=localhost
rem set java_opts=%java_opts% -Dtangosol.coherence.proxy.port=21000

set java_opts=%java_opts% -Dhazelcast.logging.type=slf4j -Dlogback.configurationFile=hz-client-logging.xml
set java_opts=%java_opts% -Dlog.name=tpox-client -Dhz.log.level=warn -Dbdb.log.level=info

set java_opts=%java_opts% -Dhazelcast.client.event.thread.count=1

set java_opts=%java_opts% -Dbdb.schema.address=%schema_addr%
set java_opts=%java_opts% -Dbdb.schema.name=%schema%
set java_opts=%java_opts% -Dbdb.schema.user=guest
set java_opts=%java_opts% -Dbdb.schema.password=password

rem possible values are: member, owner, any
set java_opts=%java_opts% -Dbdb.client.submitTo=owner
set java_opts=%java_opts% -Dbdb.client.bufferSize=32
set java_opts=%java_opts% -Dbdb.client.fetchSize=10
set java_opts=%java_opts% -Dbdb.client.connectAttempts=3
set java_opts=%java_opts% -Dbdb.client.loginTimeout=30
set java_opts=%java_opts% -Dbdb.client.smart=true
set java_opts=%java_opts% -Dbdb.client.poolSize=10
set java_opts=%java_opts% -Dbdb.client.healthCheck=skip
set java_opts=%java_opts% -Dbdb.client.customAuth=true
set java_opts=%java_opts% -Dbdb.client.queryCache=true

rem set java_opts=%java_opts% -Dbdb.client.storeMode=merge
rem set java_opts=%java_opts% -Dbdb.client.txTimeout=100
set java_opts=%java_opts% -Dbdb.client.txLevel=skip
rem set java_opts=%java_opts% -Dbdb.document.data.format=XML
rem set java_opts=%java_opts% -Dbdb.document.map.merge=false

set java_opts=%java_opts% -Duser.country=US -Duser.language=en

exit /b


