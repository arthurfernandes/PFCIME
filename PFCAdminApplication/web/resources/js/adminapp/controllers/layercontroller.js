adminApp.controller('LayerController',['$scope','LayerHandler','$log', function($scope,layerHandler,$log) {
    $scope.layers = [{wmsId : 'bairros',features : [{wmsId : "abcd"},{name: "abdef"}]},{wmsId : 'hoteis'},{wmsId : 'atracoes'}];
    $scope.selected = {};
    $scope.currentsave = false;
    
    //UI Related
    
    $scope.select = function(index){
        $scope.selected = angular.copy($scope.layers[index]);
        $scope.currentsave = true;
    };
    
    $scope.addItemAction = function(index){
        $scope.selected = {};
        $scope.currentsave = false;
    };
    
    $scope.deleteSelectedFeature = function(index){
        $scope.selected.features.splice(index,1);
    };
    
    $scope.addSelectedFeature = function(index){
        $scope.selected.features.push({wmsId : "",name : ""});
    };
    
    //CRUD
    
    $scope.readAll = function(){
        
    };
    
    $scope.add = function(){
        
    };
    
    $scope.save = function(){
        
    };
    
    $scope.delete = function(wmsId){
        
    };
    
}]);