<%--
  Created by IntelliJ IDEA.
  User: 单继重
  Date: 2016/11/16
  Time: 11:05
  Description:
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<form action="/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="file"><br><br>
    <input type="hidden" name="name">
    <input type="submit" value="上传">
</form>
</body>
</html>