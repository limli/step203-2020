package com.google.step.datamanager;

import com.google.step.model.Comment;

public interface CommentManager {
  public Comment createComment(long dealId, long userId, String content);

  /** Gets the list of comments with the given dealId */
  public CommentsWithToken getCommentsForDeal(long dealId);

  /** Gets more comments with the given dealId from the pagination token */
  public CommentsWithToken getCommentsForDeal(long dealId, String token);

  public Comment updateComment(long id, String content);

  public void deleteComment(long id);
}
