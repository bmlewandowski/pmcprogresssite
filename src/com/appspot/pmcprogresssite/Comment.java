package com.appspot.pmcprogresssite;

import com.google.appengine.api.datastore.Text;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import java.util.Date;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Comment {
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	Long id;
	
	@Persistent
	Long ownerid;
	
	@Persistent
	String ownername;

	@Persistent
	String ownerthumb;
	
	@Persistent
	String entitytype;
	
	@Persistent
	Text message;
	
	@Persistent
	String target;
	
	@Persistent
	Long targetid;
	
	@Persistent
	Long targetowner;
	
	@Persistent
	Long replyid;
	
	@Persistent
	boolean userhidden;	
	
	@Persistent
	boolean systemhidden;	

	@Persistent
	int flagcount;	
	
	@Persistent
	int likecount;	
	
	@Persistent
	int followcount;	
	
	@Persistent
	java.util.Date created;
	
	@Persistent
	java.util.Date modified;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getownerid() {
		return ownerid;
	}

	public void setownerid(Long ownerid) {
		this.ownerid = ownerid;
	}
	
	public String getownername() {
		return ownername;
	}

	public void setownername(String ownername) {
		this.ownername = ownername;
	}
	
	public String getownerthumb() {
		return ownerthumb;
	}

	public void setownerthumb(String ownerthumb) {
		this.ownerthumb = ownerthumb;
	}
	
	public String getentitytype() {
		return entitytype;
	}

	public void setentitytype(String entitytype) {
		this.entitytype = entitytype;
	}
	
	public Text getmessage() {
		return message;
	}

	public void setmessage(Text message) {
		this.message = message;
	}
	
	public String gettarget() {
		return target;
	}

	public void settarget(String target) {
		this.target = target;
	}
	
	public Long gettargetid() {
		return targetid;
	}

	public void settargetid(Long targetid) {
		this.targetid = targetid;
	}
	
	public Long gettargetowner() {
		return targetowner;
	}

	public void settargetowner(Long targetowner) {
		this.targetowner = targetowner;
	}
	
	public Long getreplyid() {
		return replyid;
	}

	public void setreplyid(Long replyid) {
		this.replyid = replyid;
	}
	
	public boolean getuserhidden() {
		return userhidden;
	}

	public void setuserhidden(boolean userhidden) {
		this.userhidden = userhidden;
	}

	public boolean getsystemhidden() {
		return systemhidden;
	}

	public void setsystemhidden(boolean systemhidden) {
		this.systemhidden = systemhidden;
	}

	public int getflagcount() {
		return flagcount;
	}

	public void setflagcount(int flagcount) {
		this.flagcount = flagcount;
	}	
	
	public int getlikecount() {
		return likecount;
	}

	public void setlikecount(int likecount) {
		this.likecount = likecount;
	}		
	
	public int getfollowcount() {
		return followcount;
	}

	public void setfollowcount(int followcount) {
		this.followcount = followcount;
	}
	
	public Date getcreated() {
		return created;
	}

	public void setcreated(Date created) {
		this.created = created;
	}	
	
	public Date getmodified() {
		return modified;
	}

	public void setmodified(Date modified) {
		this.modified = modified;
	}
}
