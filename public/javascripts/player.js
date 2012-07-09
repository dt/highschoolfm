var localQueue = [];
var playing = false;
var currentlyPlaying = false;
var skipping = false;
var loader = new Image();
$(document).ready(function() {
  $('#api').bind('ready.rdio', function() {
    var self = this;

    $('#queue .stream').each(function() {
      var trackKey = $(this).attr('id');
      $(this).addClass("enqueued");
      localQueue.push(trackKey);
    });
    var first = localQueue.shift();
    $(this).rdio().play(first);
  });
  $('#api').bind('playingTrackChanged.rdio', function(e, playingTrack, sourcePosition) {
    if (playingTrack && currentlyPlaying != playingTrack.key) {
      currentlyPlaying = playingTrack.key;
      console.log("playing " + playingTrack.key);

      playing = true;
      loader.src = playingTrack.icon;
      var nowPlaying = $('#' + playingTrack.key).addClass("playing");
      nowPlaying.prevAll().slideUp().remove();
      nowPlaying.find('title').text(playingTrack.name);
      nowPlaying.find('artist').text(playingTrack.artist);
      $('#art').fadeOut(500, function() {
        $('#art').attr('src', playingTrack.icon).delay(200).fadeIn(500);
      });

    }
    });

  $('#api').bind('playStateChanged.rdio', function(e, playState) {
    console.log("state " + playState)
    if (playState == 2) {
      var wasPlaying = $('li.playing');
      wasPlaying.prevAll().slideUp(function() {$(this).remove();});
      wasPlaying.removeClass("playing").slideUp(function() {$(this).remove();});

      if (skipping) {
          skipping = false;
      } else {
        if (playing) {
          playing = false;
          $('#api').rdio().play(localQueue.shift());
        } else {
          playing = false;
        }
      }
    } else if (playState == 0) { // paused
      $('#play').show();
      $('#pause').hide();
    } else {
      $('#play').hide();
      $('#pause').show();
    }
  });

  // this is a valid playback token for localhost.
  // but you should go get your own for your own domain.
  $('#api').rdio(playbackToken);
  $('#play').click(function() { $('#api').rdio().play(); });
  $('#pause').click(function() { $('#api').rdio().pause(); });
  $('#next').click(function() {
    skipping = true;
    $('#api').rdio().play(localQueue.shift());
  });
});