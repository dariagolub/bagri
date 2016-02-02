@echo off
@
@
setlocal

call set-ycsb-env.cmd

rem The number of securities is fixed to 20,833.  Therefore, either 
rem "-u 83 -tr 251"  or "-u 251 -tr 83" can be use to insert the 20833
rem security documents  (because 83 * 251 = 20833). 4166*5 = 3472*6 = 2976*7 = 2604*8 = 2314*9

rem register schema & index
rem "%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.bagri.common.manage.MBeanInvoker %admin_addr% %login% %password% init_default_schema.xml

rem insert securities to the cache
"%java_exec%" -server -showversion %java_opts% -cp "%app_home%\config\*;%app_home%\lib\*" com.yahoo.ycsb.Client -load -threads 50 -P workloada

rem perform queries loopig by user count
for /l %%x in (50, 10, 200) do (
 	"%java_exec%" -server %java_opts% -cp "%app_home%\config\*;%app_home%\lib\*" com.yahoo.ycsb.Client -threads %%x -P workloada  
)

goto exit

:instructions

echo Usage:
echo %app_home%\ycsb-loada.cmd
goto exit

:exit
endlocal
@echo on

