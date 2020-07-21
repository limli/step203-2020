let dealId;
let votes = 0;
let myVote = 0;

/**
 * Calls the backend to get the list of comments and loads it to the page
 */
function initComments() {
  document.getElementById('dealId-input').value = dealId;
  $.ajax({
    url: '/api/comments',
    data: {
      dealId: dealId,
    },
  }).done((comments) => {
    loadCommentsToPage(comments);
  });
}

/**
 * Loads the deal onto the page
 * @param {object} deal
 */
function loadDealDataToPage(deal) {
  $('#deal-loading').hide();
  $('#deal-page').show();

  const dealTitleElement = document.getElementById('deal-title');
  dealTitleElement.innerText = deal.description;

  const dealImageElement = document.getElementById('deal-image');
  dealImageElement.src = deal.image;

  const dealInfoElement = document.getElementById('deal-info');
  dealInfoElement.innerText = deal.description;

  const dealRestaurantElement = document.getElementById('restaurant-info');
  dealRestaurantElement.innerText = deal.restaurant.name;
  dealRestaurantElement.href = '/restaurant/' + deal.restaurant.id;

  const dealValidStart = document.getElementById('start-date');
  dealValidStart.innerText = deal.start;
  const dealValidEnd = document.getElementById('end-date');
  dealValidEnd.innerText = deal.end;

  const dealPoster = document.getElementById('user-poster');
  dealPoster.href = '/user/' + deal.poster.id;
  dealPoster.innerText = deal.poster.username;

  const dealSource = document.getElementById('deal-source');
  dealSource.innerText = deal.source;
  dealSource.href = deal.source;

  const voteElement = document.getElementById('votes-num');
  votes = deal.votes;
  voteElement.innerText = deal.votes;

  dealId = deal.id;
}

/**
 * Get comments for a deal
 * @param {array} comments
 */
function loadCommentsToPage(comments) {
  const commentListElement = document.getElementById('comment-list');
  commentListElement.innerHTML = '';
  comments.forEach((comment) => {
    commentListElement.appendChild(createCommentElement(comment));
  });
}

/**
 * Creates comment element
 * @param {object} comment
 * @return {object} commentElement
 */
function createCommentElement(comment) {
  const commentElement = document.createElement('div');
  commentElement.className = 'border border-info py-3 px-3 my-3';

  const textElement = document.createElement('div');
  textElement.innerText = comment.user.username +
  ': ' + comment.content;
  commentElement.appendChild(textElement);

  const timeElement = document.createElement('small');
  timeElement.className = 'text-muted';
  const date = new Date(Date.parse(comment.timestamp));
  timeElement.innerText = 'Posted on: ' + date.toString();
  commentElement.appendChild(timeElement);

  return commentElement;
}

/**
 * Updates vote UI based on global variable vote and myVote
 */
function updateMyVote() {
  const upvoteBtn = document.getElementById('upvote-btn');
  const downvoteBtn = document.getElementById('downvote-btn');
  upvoteBtn.classList.remove('active');
  downvoteBtn.classList.remove('active');
  if (myVote > 0) {
    upvoteBtn.classList.add('active');
  } else if (myVote < 0) {
    downvoteBtn.classList.add('active');
  }
  const voteElement = document.getElementById('votes-num');
  voteElement.innerText = votes + myVote;
}

/**
 * Calls backend and POSTs vote to deal
 * @param {number} dir
 */
function postVote(dir) {
  $.ajax({
    url: '/api/vote/' + dealId,
    method: 'POST',
    data: {
      dir: dir,
    },
  });
}

/**
 * Called when the user clicks the upvote button
 */
function handleUpvote() {
  if (myVote == 1) {
    myVote = 0;
    postVote(0);
  } else {
    myVote = 1;
    postVote(1);
  }
  updateMyVote();
}

/**
 * Called when the user clicks the downvote button
 */
function handleDownvote() {
  if (myVote == -1) {
    myVote = 0;
    postVote(0);
  } else {
    myVote = -1;
    postVote(-1);
  }
  updateMyVote();
}

/**
 * Display Deal Not Found on the page
 */
function showNotFound() {
  $('#deal-loading').hide();
  $('#deal-notfound').show();
}

/**
 * Calls backend to get user's current vote status, and shows the
 * upvote/downvote buttons
 */
function initVotes() {
  $.ajax('/api/vote/' + dealId)
      .done((dir) => {
        myVote = parseInt(dir);
        votes -= myVote; // exclude myVote from global vote count
        const voteDiv = document.getElementById('vote-div');
        voteDiv.style.display = 'block';
        updateMyVote();
      });
}

/**
 * Shows the form to edit the deal, and hides the deal details
 */
function handleEdit() {
  $('#deal-details').hide();
  $('#edit-form').show();
}

/**
 * Shows the deal details and hides the form
 */
function handleCancelEdit() {
  $('#deal-details').show();
  $('#edit-form').hide();
}

/**
 * Calls backend for data on deal
 */
function initDeal() {
  const myPath = window.location.pathname; // path is /deals/<id>
  const myId = myPath.substr(7);
  $.ajax('/api/deals/' + myId)
      .done((deal) => {
        loadDealDataToPage(deal);
        initComments();
        initVotes();
      })
      .fail(() => {
        showNotFound();
      });
}

addLoadEvent(() => {
  initDeal();
});
