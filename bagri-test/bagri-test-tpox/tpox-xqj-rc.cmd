@echo off
@
@
setlocal

call set-tpox-env.cmd

rem The number of securities is fixed to 20,833.  Therefore, either 
rem "-u 83 -tr 251"  or "-u 251 -tr 83" can be use to insert the 20833
rem security documents  (because 83 * 251 = 20833). 4166*5 = 3472*6 = 2976*7 = 2604*8 = 2314*9

rem register schema & index
rem "%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.bagri.common.manage.MBeanInvoker %admin_addr% %login% %password% init_default_schema.xml

rem insert securities to the cache
rem "%java_exec%" -server -showversion %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" net.sf.tpox.workload.core.WorkloadDriver -w queries/XQJ/insSecurity.xml -tr 4167 -u 5
"%java_exec%" -server -showversion %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" net.sf.tpox.workload.core.WorkloadDriver -w queries/XQJ/insSecurity.xml -tr 4167 -u 5 -v 1

rem get insert statistics
"%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.bagri.test.tpox.StatisticsCollector %admin_addr% %schema% QueryManagement executeXQuery InsertSecurities ./stats.txt false

rem perform queries loopig by user count
for /l %%x in (1, 1, 10) do (
 	"%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" net.sf.tpox.workload.core.WorkloadDriver -w queries/XQJ/securities.xml -u %%x -v 1
	"%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.bagri.test.tpox.StatisticsCollector %admin_addr% %schema% QueryManagement executeXQuery Users=%%x ./stats.txt false
)

rem "%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" net.sf.tpox.workload.core.WorkloadDriver -w queries/XQJ/securities.xml -tr 100 

goto exit

:instructions

echo Usage:
echo %app_home%\tpox-xqj-sec.cmd
goto exit

:exit
endlocal
@echo on
