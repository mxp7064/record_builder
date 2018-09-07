How to run:

1) Run database_dump.sql script in MySQL to create the database 

2) Edit /executable/config.properties and set database user, password and port number

3) With command line change directory to /record_builder/executable and run the jar file with: java -jar record_builder-1.0.jar

4) There are 2 sample files in /executable/sample_csv_files which you can add to executable/incoming or you can add your own files

5) Check out the output in the command line

Note: Tested on Windows 10 with Java 1.8

Note: Project source files are in the record_builder subdirectory