package com.appspot.pmcprogresssite;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

import java.util.Date;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class User implements Serializable {
	
	private static final long serialVersionUID = 4674810812956470269L;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	Long id;

	@Persistent
	String entitytype;
	
	@Persistent
	String email;

	@Persistent
	String displayname;
	
	@Persistent
	Text description;

	@Persistent
	String password;

	@Persistent
	String portrait;
	
	@Persistent
	String thumbnail;
		
	// Temp attribute for image data transfer
	Text thumbdata;
	
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
	
	public String getemail() {
		return email;
	}

	public void setemail(String email) {
		this.email = email;
	}

	public String getdisplayname() {
		return displayname;
	}

	public void setdisplayname(String displayname) {
		this.displayname = displayname;
	}

	public Text getdescription() {
		return description;
	}

	public void setdescription(Text description) {
		this.description = description;
	}
	
	public String getpassword() {
		return password;
	}

	public void setpassword(String password) {
		this.password = password;
	}

	public String getportrait() {
		return portrait;
	}

	public void setportrait(String portrait) {
		this.portrait = portrait;
	}
	
	public String getthumbnail() {
		return thumbnail;
	}

	public void setthumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public Text getthumbdata() {
		return thumbdata;
	}

	public void setthumbdata(Text thumbdata) {
		this.thumbdata = thumbdata;
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