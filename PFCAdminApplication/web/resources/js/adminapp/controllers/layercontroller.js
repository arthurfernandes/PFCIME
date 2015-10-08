adminApp.controller('LayerController',['$scope','LayerHandler','$log', function($scope,layerHandler,$log) {
    $scope.layers = [{wmsId : 'bairros',features : [{wmsId : "abcd"},{name: "abdef"}]},{wmsId : 'hoteis'},{wmsId : 'atracoes'}];
    $scope.selected = {};
    $scope.currentsave = false;
    $scope.readLayersState = false;
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
    
    $scope.readLayersIfVisible = function(){
        if($scope.readLayerState){
            $scope.readAll();
        }
        
        $scope.readLayerState = !$scope.readLayerState;
    };
    
    //FORM VALIDATION
    
    $scope.validateSelected = function(){
        if($scope.selectedElement.wmsId === ""){
            
        }
        
    };
    
    //CRUD
    
    $scope.readAll = function(){
        console.log("readall");
        var promise = layerHandler.readAll();
        promise.then(function successCallBack(response){
            console.log("success");
            if(typeof response.data === 'object'){
                $scope.layers = response.data.objects;
            }
        });
    };
    
    $scope.add = function(){
        validateSelected();
        layerHandler.add($scope.selectedElement);
    };
    
    $scope.save = function(){
        
    };
    
    $scope.delete = function(wmsId){
        
    };
    
}]);