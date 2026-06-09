@echo off
echo ========================================
echo Copilot-Search-8.x 编译验证
echo ========================================
echo.

cd /d D:\Learning\awesome-copilot

echo [1/2] 清理项目...
call mvn clean -pl copilot-search-8.x -am

echo.
echo [2/2] 编译 copilot-search-8.x 模块...
call mvn compile -pl copilot-search-8.x -am -DskipTests

echo.
echo ========================================
echo 编译完成!
echo ========================================
pause
