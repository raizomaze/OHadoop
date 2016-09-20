package hello;

import java.util.Arrays;

public class HadoopMRDetails 
{
	private String dargs;

	private String hadoopjar;

	private String mainclass;

	private String[] args;

	private String libjar;

	public String getDargs() 
	{
		return dargs;
	}

	public void setDargs(String dargs) 
	{
		this.dargs = dargs;
	}

	public String getHadoopjar() 
	{
		return hadoopjar;
	}

	public void setHadoopjar(String hadoopjar) 
	{
		this.hadoopjar = hadoopjar;
	}

	public String getMainclass()
	{
		return mainclass;
	}

	public void setMainclass(String mainclass) 
	{
		this.mainclass = mainclass;
	}

	public String[] getArgs() 
	{
		return args;
	}

	public void setArgs(String[] args)
	{
		this.args = args;
	}

	public String getLibjar() {
		return libjar;
	}

	public void setLibjar(String libjar) 
	{
		this.libjar = libjar;
	}

	@Override
	public String toString()
	{
		return "HadoopMRDetails [dargs=" + dargs + ", hadoopjar=" + hadoopjar + ", mainclass=" + mainclass + ", args="
				+ Arrays.toString(args) + ", libjar=" + libjar + "]";
	}
	
	
}