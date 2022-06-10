package com.appspot.pmcprogresssite;

import com.google.appengine.api.datastore.Text;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Step {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	Long id;
	
	@Persistent
	String entitytype;
	
	@Persistent
	String title;

	@Persistent
	Text description;
	
	@Persistent
	String mediatype;
	
	@Persistent
	Long projectid;
	
	@Persistent
	Long ownerid;
	
	@Persistent
	String ownername;
	
	@Persistent
	String ownerthumb;

	@Persistent
	String thumbnail;
	
	@Persistent
	String thumbname;
	
	// Temp attribute for image data transfer
	Text thumbdata;
	
	@Persistent
	int sortorder;
	
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
	int commentcount;		
	
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

	public String getentitytype() {
		return entitytype;
	}

	public void setentitytype(String entitytype) {
		this.entitytype = entitytype;
	}
	
	public String gettitle() {
		return title;
	}

	public void settitle(String title) {
		this.title = title;
	}

	public Text getdescription() {
		return description;
	}

	public void setdescription(Text description) {
		this.description = description;
	}

	public String getmediatype() {
		return mediatype;
	}

	public void setmediatype(String mediatype) {
		this.mediatype = mediatype;
	}
	
	public Long getprojectid() {
		return projectid;
	}

	public void setprojectid(Long projectid) {
		this.projectid = projectid;
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
	
	public String getthumbnail() {
		return thumbnail;
	}

	public void setthumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getthumbname() {
		return thumbname;
	}

	public void setthumbname(String thumbname) {
		this.thumbname = thumbname;
	}
	
	public Text getthumbdata() {
		return thumbdata;
	}

	public void setthumbdata(Text thumbdata) {
		this.thumbdata = thumbdata;
	}
	
	public int getsortorder() {
		return sortorder;
	}

	public void setsortorder(int sortorder) {
		this.sortorder = sortorder;
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
	
	public int getcommentcount() {
		return commentcount;
	}

	public void setcommentcount(int commentcount) {
		this.commentcount = commentcount;
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