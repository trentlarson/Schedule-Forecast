


function drawAllLinks(elemName) {
  jg = new jsGraphics(elemName);
  outerElems = $('#' + elemName);
  outerElems.each(function() {
    $('span', this).each(function(){
      var sourceElem=$(this);
      if(sourceElem.attr('rel')) {
        if (sourceElem.attr('type') == 'successor') {
          var destElem=$('#'+sourceElem.attr('rel'));
          if(destElem.length) {
            line1X = sourceElem.offset().left + sourceElem.width() / 2;
            topY = sourceElem.offset().top + sourceElem.height() / 2;
            bottomY = destElem.offset().top + destElem.height() / 2;
            jg.drawLine
              (line1X,
               topY,
               line1X,
               bottomY);
            jg.drawLine
              (line1X,
               bottomY,
               destElem.offset().left + destElem.width(),
               destElem.offset().top + destElem.height() / 2);
          }
        }
        if (sourceElem.attr('type') == 'subtask') {
          var destElem=$('#'+sourceElem.attr('rel'));
          if(destElem.length) {
            jg.drawLine
              (sourceElem.offset().left + sourceElem.width() / 2,
               sourceElem.offset().top + sourceElem.height() / 2,
               destElem.offset().left + destElem.width() / 2,
               destElem.offset().top + destElem.height() / 2);
          }
        }
      }
    })
  })
  jg.paint();
}

