adminApp.factory('LayerHandler',['$http',function($http){
    var layerHandlerURL = "layer-handler";
    var readAllURL = layerHandlerURL +"?action=readAll";
    var addURL = layerHandlerURL+"?action=add";
    var saveURL = layerHandlerURL+"?action=save";
    var deleteURL = layerHandlerURL+"?action=delete";
    
    return {
        readAll : function(){
            return $http({
                method : 'GET',
                url : readAllURL
            });
        },
        
        add : function(){
            
        },
        
        save : function(){
            
        },
        
        delete : function(){
            
        }
    };
}]);