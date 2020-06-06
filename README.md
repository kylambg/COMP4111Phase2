# COMP4111Phase2
1. Go to SQL_used file to set up the database as stated
2. Change the user and password to format userXXXXX and passwdXXXXX
3. The default port for DB is 8080
4. I haven't test my server on Linux, regarding to case-sensitive issue, please modify this line
    <code>final static String URL = "jdbc:mysql://localhost:3306/Comp4111"; //DBMS schema name</code>
    to "comp4111", or basing on the schema name you have made in Connector.java

Mark the lib folder as the library used, there should be Jackson 2.10, Apache HttpCore NIO 4.13 and SQLconnector. You may directly import it as the library.
Inside the lib folder, there is also a file named src, please import it as the library used
