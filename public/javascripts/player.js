var loader = new Image();

var ignoreStopped = false;
var currentTrack = false;

function skipTo(id) {
  ignoreStopped = true;
  R.player.play({source: id});
}

function playNext() {
  var next = $('#queue .enqueued').first().attr('id');
  R.player.play({source: next});
}

function nowPlaying(id, art, artist, title) {
  loader.src = art;
  var  started = $('#' + id).addClass("playing").removeClass('enqueued');
  started.prevAll().slideUp( function() { $(this).remove(); });
  started.find('title').text(title);
  started.find('artist').text(artist);
  $('#artBox').fadeOut(500, function() {
    $('#art').attr('src', art)
    $(this).delay(200).fadeIn(500);
  });
}

$(document).ready(function() {
  R.ready(function() {
    $('#queue .enqueued').each(function() {
      $(this).click(function() { skipTo($(this).attr('id')); });
    });

    R.player.on('change:playingTrack', function(newTrack) {
      var playingTrack = newTrack.attributes
      console.log("new playing track", playingTrack)
      if (playingTrack && (currentTrack != playingTrack.key)) {
        currentTrack = playingTrack.key;
        nowPlaying(playingTrack.key, playingTrack.icon, playingTrack.artist, playingTrack.name);
      }
    });

    R.player.on('change:playState', function(playState) {
      console.log("new playing state", playState)

      if (playState == 2 && currentTrack) {
        if (ignoreStopped) {
        } else {
          playNext();
        }
        currentTrack = false;
        ignoreStopped = false;
      } else if (playState == 0) { // paused
        $('#artBox').attr('class', 'paused');
      } else {
        $('#artBox').attr('class', 'playing');
      }
    });

    $('#play').click(function() { R.player.play(); });
    $('#pause').click(function() { R.player.pause(); });

    playNext();
  });
});