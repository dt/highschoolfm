var loader = new Image();

var ignoreStopped = false;
var currentTrack = false;

function skipTo(id) {
  ignoreStopped = true;
  $('#api').rdio().play(id);
}

function playNext() {
  var next = $('#queue .enqueued').first().attr('id');
  $('#api').rdio().play(next);
}

function nowPlaying(id, art, artist, title) {
  loader.src = art;
  var  started = $('#' + id).addClass("playing").removeClass('enqueued');
  started.prevAll().slideUp( function() { $(this).remove(); });
  started.find('title').text(title);
  started.find('artist').text(artist);
  $('#art').fadeOut(500, function() { $('#art').attr('src', art).delay(200).fadeIn(500);});
}

$(document).ready(function() {
  $('#api').bind('ready.rdio', function() {
    $('#queue .enqueued').each(function() {
      $(this).click(function() { skipTo($(this).attr('id')); });
    });
    playNext();
  });

  $('#api').bind('playingTrackChanged.rdio', function(e, playingTrack, sourcePosition) {
    if (playingTrack && (currentTrack != playingTrack.key)) {
      currentTrack = playingTrack.key;
      nowPlaying(playingTrack.key, playingTrack.icon, playingTrack.artist, playingTrack.name);
    }
  });

  $('#api').bind('playStateChanged.rdio', function(e, playState) {
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

  // this is a valid playback token for localhost.
  // but you should go get your own for your own domain.
  $('#api').rdio(playbackToken);
  $('#play').click(function() { $('#api').rdio().play(); });
  $('#pause').click(function() { $('#api').rdio().pause(); });
});