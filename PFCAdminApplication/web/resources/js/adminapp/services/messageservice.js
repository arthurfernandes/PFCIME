adminApp.factory('messageService',['',function(){
   var _messageWidget = jQuery("<messageWidget></messageWidget>");
   return {
       alertMessage : function(level,title,message){
           
       }
   }     
}]);
/*
adminApp.directive('messageWidget',function(){
    return {
        restrict : 'E',
        scope : {
            message : "@message",
            title : "@title",
            level : "@level"
        }
        template
    }
});
*/