adminApp.directive('jqslidable', function(){
    var slideEffectTimer = 400;
    
    return {
        restrict : 'AC',
        link : function(scope, element, attrs){
            var closestPanel = jQuery(element).closest(".panel");
            var panelBody = closestPanel.find(".panel-body").first();
            jQuery(element).click(function(){
                panelBody.slideToggle(slideEffectTimer);
            });
        }
    };
});


