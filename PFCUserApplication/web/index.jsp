<%-- 
    Document   : index.jsp
    Created on : 28/07/2015, 19:30:40
    Author     : arthurfernandes
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <title>PFC IME</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/resources/logo/icon.png"><!-- Major Browsers -->
<!--[if IE]><link rel="SHORTCUT ICON" href="${pageContext.request.contextPath}/resources/logo/icon.ico"/><![endif]--><!-- Internet Explorer-->
        <script src="${pageContext.request.contextPath}/resources/js/code.jquery.com_jquery-2.1.4.min.js"></script>
        <script src="${pageContext.request.contextPath}/resources/bootstrap-3.3.5-dist/js/bootstrap.min.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/bootstrap-3.3.5-dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/font-awesome-4.4.0/css/font-awesome.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/indexpage.css">
    </head>
    <body>
        <div class="container">
            <div class="row">
                <div class="col-sm-6 col-md-6 col-lg-5 hidden-xs iphone">
                    <img src="resources/img/iphone.png" alt="Celular exibindo mapa.">
                </div>
                <div class="col-sm-5 col-md-5 col-lg-5 col-md-offset-1 col-sm-offset-1">
                    <div class="panel panel-default">
                        <div class="panel-heading"> <strong class=""><h2>PFC IME</h2></strong>
                        </div>
                        <div class="panel-body">
                            <form class="form-horizontal" role="form" id="login-form" method="POST">
                                <div class="form-group">
                                    <label for="inputEmail3" class="col-sm-3 control-label">Usuário</label>
                                    <div class="col-sm-9">
                                        <input id="login-username" class="form-control" name="username" placeholder="usuario">
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label for="inputPassword3" class="col-sm-3 control-label">Senha</label>
                                    <div class="col-sm-9">
                                        <input id="login-password" type="password" class="form-control" name="password" placeholder="senha">
                                    </div>
                                </div>
                                <div class="form-group">
                                    <div class="col-sm-offset-3 col-sm-9">
                                        <div class="checkbox">
                                            <label class="">
                                                <input type="checkbox" class="">Mantenha-me conectado</label>
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group last ">
                                    <div class="col-sm-offset-3 col-sm-9">
                                        <button type="submit" class="btn btn-success col-xs-12">Entrar</button>
                                    </div>
                                </div>
                                <div class="col-sm-offset-3 col-sm-9">
                                    <span role="alert" class="text-danger" id="login-message" style="display:none"></span>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script>
            var loginModule = function(){
                //INIT
                var message = null;
                var serverURL="login";
                //CACHE DOM
                var $loginForm = $("#login-form");
                var $usernameField = $loginForm.find("#login-username");
                var $passwordField = $loginForm.find("#login-password");
                var $messageBox = $("#login-message");

                //BIND EVENTS

                $loginForm.submit(function(e){
                    e.preventDefault();
                    message = null;
                    if(_validateInput()){
                        var jqXHR = _loginToServer($usernameField.val(),$passwordField.val());
                        jqXHR.done(function(data){
                                
                                window.location.replace("map");
                                
                        });
                        jqXHR.fail(function(jqXHR,exception){
                            if(jqXHR.status === 302){
                                console.log("redirect fail");
                            }
                            console.log("Login failed"+jqXHR.status);
                            if (jqXHR.status === 0) {
                               message = 'Não foi possível se conectar ao servidor';
                            } else if(jqXHR.status == 400){
                               message = "Usuário ou senha incorretos";
                            }else if(jqXHR.status == 401 || jqXHR.status == 403) {
                               message = "Usuário ou senha incorretos";
                            } else if (jqXHR.status == 404) {
                                message = 'O servidor não possui a página requisitada';
                            } else if (jqXHR.status == 500) {
                                message = 'O servidor apresentou um problema';
                            } else if (exception === 'timeout') {
                                message = 'Timeout: O servidor não respondeu.';
                            } else if (exception === 'abort') {
                                message = 'A requisição foi abortada';
                            } else {
                                message = 'Ocorreu um problema não identificado.';
                            }
                        });
                        jqXHR.always(function(){
                            _render();
                        });
                    }
                    _render();
                });

                var _validateInput = function(){
                    $usernameField.removeClass("red-border");
                    $passwordField.removeClass("red-border");
                    if($usernameField.val() === ""){
                        message = "Preencha o usuário.";
                        $usernameField.addClass("red-border");
                        return false;
                    }
                    else if($passwordField.val() === ""){
                        $passwordField.addClass("red-border");
                        message = "Preencha a senha.";
                        return false;
                    }
                    return true;
                };

                var _loginToServer = function(username,password){
                    return $.post(serverURL,{username : username,password : password});
                };

                var _render = function(){
                    if(message === null){
                        $messageBox.hide();
                    }
                    else{
                        $messageBox.text(message);
                        $messageBox.show();
                    }
                };

                return{
                    loginToServer : _loginToServer
                };

            }();
            
        </script>
        
    </body>
</html>

